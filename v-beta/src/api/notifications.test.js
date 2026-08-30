import {
  fetchAllNotifications,
  fetchUnreadNotifications,
} from "./notifications";

const user = {
  getIdToken: jest.fn().mockResolvedValue("token"),
};

const reportCreated = {
  notificationId: 81,
  summary: {
    eventTypeName: "REPORT_CREATED",
    description: "A user submitted a content report",
  },
  createdAt: "2026-08-14T19:11:00Z",
};

const contentRemoved = {
  notificationId: 83,
  summary: {
    eventTypeName: "CONTENT_REMOVED",
    description: "One of your content had been reported and removed.",
  },
  createdAt: "2026-08-18T18:00:00Z",
};

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => (typeof body === "string" ? body : JSON.stringify(body)),
  };
}

describe("notifications API", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    jest.clearAllMocks();
    user.getIdToken.mockResolvedValue("token");
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it("polls unread rows from GET /api/notification/short", async () => {
    global.fetch.mockResolvedValue(jsonResponse([reportCreated]));
    await expect(fetchUnreadNotifications(user)).resolves.toEqual([reportCreated]);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/notification/short"),
      expect.objectContaining({
        headers: { Authorization: "Bearer token" },
      }),
    );
  });

  it("pages GET /api/notification/all and overlays unread ids from /short", async () => {
    global.fetch.mockImplementation((url) => {
      const href = String(url);
      if (href.includes("/api/notification/all?offset=2")) {
        return Promise.resolve(jsonResponse([reportCreated, contentRemoved]));
      }
      if (href.includes("/api/notification/short")) {
        return Promise.resolve(jsonResponse([reportCreated]));
      }
      return Promise.resolve(jsonResponse([], 404));
    });

    const items = await fetchAllNotifications(user, 2);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/notification/all?offset=2"),
      expect.objectContaining({
        headers: { Authorization: "Bearer token" },
      }),
    );
    expect(items[0].readAt).toBeNull();
    expect(items[1].readAt).toBe("read");
  });

  it("defaults to page 1", async () => {
    global.fetch.mockResolvedValue(jsonResponse([]));
    await fetchAllNotifications(user);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/notification/all?offset=1"),
      expect.any(Object),
    );
  });

  it("throws when the all-inbox request fails", async () => {
    global.fetch.mockImplementation((url) => {
      const href = String(url);
      if (href.includes("/api/notification/all")) {
        return Promise.resolve(jsonResponse("User is not found", 404));
      }
      return Promise.resolve(jsonResponse([]));
    });
    await expect(fetchAllNotifications(user, 1)).rejects.toThrow("User is not found");
  });
});
