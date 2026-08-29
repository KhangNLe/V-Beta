import { fetchReportQueue, resolveReports } from "./reports";

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

describe("reports API queue helpers", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    jest.clearAllMocks();
    user.getIdToken.mockResolvedValue("token");
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it("loads GET /api/report/reports", async () => {
    global.fetch.mockResolvedValue(jsonResponse({ reports: [] }));
    await expect(fetchReportQueue(user)).resolves.toEqual({ reports: [] });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/report/reports"),
      expect.objectContaining({
        headers: { Authorization: "Bearer token" },
      }),
    );
  });

  it("loads one case with reportId", async () => {
    global.fetch.mockResolvedValue(jsonResponse({ reports: [{ queueScore: 2 }] }));
    await fetchReportQueue(user, 11);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/report/reports?reportId=11"),
      expect.any(Object),
    );
  });

  it("maps missing permission on the queue to access denied", async () => {
    global.fetch.mockResolvedValue(jsonResponse("missing", 404));
    await expect(fetchReportQueue(user)).rejects.toThrow("Access denied.");
  });

  it("maps missing reportId detail to not found", async () => {
    global.fetch.mockResolvedValue(jsonResponse("Report not found", 404));
    await expect(fetchReportQueue(user, 99)).rejects.toThrow("Report not found");
  });

  it("posts dismiss and remove decisions", async () => {
    global.fetch.mockResolvedValue(jsonResponse("", 200));
    await resolveReports(user, {
      reportIds: [11, 12],
      decision: "CONTENT_REMOVED",
      reason: "Does not belong on this wall.",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/report"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          reportIds: [11, 12],
          decision: "CONTENT_REMOVED",
          reason: "Does not belong on this wall.",
        }),
      }),
    );
  });
});
