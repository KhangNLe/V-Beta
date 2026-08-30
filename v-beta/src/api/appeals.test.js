import {
  ADMIN_REASON_MAX_LENGTH,
  APPEAL_REASON_MAX_LENGTH,
  createAppeal,
  fetchAppeals,
  fetchDeletionNotice,
  resolveAppeal,
} from "./appeals";

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

describe("appeals API", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    jest.clearAllMocks();
    user.getIdToken.mockResolvedValue("token");
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it("loads GET /api/moderate/appeal/notice", async () => {
    global.fetch.mockResolvedValue(
      jsonResponse({ reportId: 11, canAppeal: true, adminReason: "Removed." }),
    );
    await expect(fetchDeletionNotice(user, 11)).resolves.toMatchObject({
      reportId: 11,
      canAppeal: true,
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/appeal/notice?reportId=11"),
      expect.any(Object),
    );
  });

  it("posts one appeal", async () => {
    global.fetch.mockResolvedValue(jsonResponse("", 201));
    await createAppeal(user, {
      reportId: 11,
      appealReason: "This was a joke, please restore.",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/appeal"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          reportId: 11,
          appealReason: "This was a joke, please restore.",
        }),
      }),
    );
  });

  it("maps duplicate create to the API error text", async () => {
    global.fetch.mockResolvedValue(jsonResponse("Appeal already exists", 404));
    await expect(
      createAppeal(user, { reportId: 11, appealReason: "Please restore." }),
    ).rejects.toThrow("Appeal already exists");
  });

  it("caps appeal reason at 250 and admin comments at 255", () => {
    expect(APPEAL_REASON_MAX_LENGTH).toBe(250);
    expect(ADMIN_REASON_MAX_LENGTH).toBe(255);
  });

  it("patches approve and deny with ModerateAppealRequest fields", async () => {
    global.fetch.mockResolvedValue(jsonResponse("", 200));
    await resolveAppeal(user, {
      appealId: 7,
      appealStatus: "APPROVED",
      adminReason: "Restored after review.",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/appeal"),
      expect.objectContaining({
        method: "PATCH",
        body: JSON.stringify({
          appealId: 7,
          appealStatus: "APPROVED",
          adminReason: "Restored after review.",
        }),
      }),
    );

    await resolveAppeal(user, {
      appealId: 7,
      appealStatus: "DENIED",
      adminReason: "Removal stands.",
    });
    expect(global.fetch).toHaveBeenLastCalledWith(
      expect.stringContaining("/api/moderate/appeal"),
      expect.objectContaining({
        method: "PATCH",
        body: JSON.stringify({
          appealId: 7,
          appealStatus: "DENIED",
          adminReason: "Removal stands.",
        }),
      }),
    );
  });

  it("loads the admin appeal queue and one appeal by report id", async () => {
    global.fetch.mockResolvedValue(
      jsonResponse({ appeals: [{ appealId: 7, appealReason: "Please restore." }] }),
    );
    await expect(fetchAppeals(user)).resolves.toEqual({
      appeals: [{ appealId: 7, appealReason: "Please restore." }],
    });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/moderate\/appeal$/),
      expect.any(Object),
    );

    global.fetch.mockResolvedValue(
      jsonResponse({ appeals: [{ appealId: 7 }] }),
    );
    await fetchAppeals(user, { reportId: 11 });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/moderate/appeal?reportId=11"),
      expect.any(Object),
    );
  });
});
