import {
  fetchAllLogbookEntries,
  fetchLogbook,
  LOGBOOK_PAGE_SIZE,
} from "./moderation";

const user = {
  getIdToken: jest.fn().mockResolvedValue("token"),
};

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => (typeof body === "string" ? body : JSON.stringify(body)),
  };
}

describe("moderation logbook API", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    jest.clearAllMocks();
    user.getIdToken.mockResolvedValue("token");
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it("loads GET /api/moderate/logbook", async () => {
    global.fetch.mockResolvedValue(jsonResponse({ moderationLogs: [] }));
    await expect(fetchLogbook(user)).resolves.toEqual({ moderationLogs: [] });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/logbook"),
      expect.objectContaining({
        headers: { Authorization: "Bearer token" },
      }),
    );
  });

  it("pages with offSetPlace", async () => {
    global.fetch.mockResolvedValue(jsonResponse({ moderationLogs: [] }));
    await fetchLogbook(user, { offSetPlace: 2 });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/logbook?offSetPlace=2"),
      expect.any(Object),
    );
  });

  it("loads one row with moderationId", async () => {
    global.fetch.mockResolvedValue(
      jsonResponse({ moderationLogs: [{ moderationId: 40 }] }),
    );
    await fetchLogbook(user, { moderationId: 40 });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/logbook?moderationId=40"),
      expect.any(Object),
    );
  });

  it("maps missing permission to access denied", async () => {
    global.fetch.mockResolvedValue(jsonResponse("missing", 404));
    await expect(fetchLogbook(user)).rejects.toThrow("Access denied.");
  });

  it("maps missing moderationId detail to not found", async () => {
    global.fetch.mockResolvedValue(jsonResponse("Moderation not found", 404));
    await expect(fetchLogbook(user, { moderationId: 99 })).rejects.toThrow(
      "Moderation not found",
    );
  });

  it("walks pages until a short page for full export", async () => {
    const full = Array.from({ length: LOGBOOK_PAGE_SIZE }, (_, index) => ({
      moderationId: index + 1,
    }));
    global.fetch
      .mockResolvedValueOnce(jsonResponse({ moderationLogs: full }))
      .mockResolvedValueOnce(
        jsonResponse({ moderationLogs: [{ moderationId: 26 }] }),
      );
    await expect(fetchAllLogbookEntries(user)).resolves.toHaveLength(26);
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });
});
