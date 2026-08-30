import {
  canSubmitAppeal,
  getAppealStatusLabel,
  getAppealUserName,
  getDeletionAdminReason,
  getOwnerAppealReason,
  getOwnerReportCategorySummary,
  getOwnerReportReasonSummary,
  getRemovedContentSummary,
} from "./ownerAppeal";

const openNotice = {
  reportId: 11,
  reportStatus: "CONTENT_REMOVED",
  adminReason: "Does not belong on this wall.",
  canAppeal: true,
  appealStatus: null,
  report: {
    discussion: {
      discussionType: "COMMENT",
      discussionContent: "hello",
    },
    reporters: [
      {
        reportId: 11,
        reporter: { username: "sam", email: "sam@example.com" },
        categoryName: "SPAM",
        reportReason: "Spammy",
      },
    ],
  },
};

describe("ownerAppeal helpers", () => {
  it("reads admin reason and content summary", () => {
    expect(getDeletionAdminReason(openNotice)).toBe("Does not belong on this wall.");
    expect(getRemovedContentSummary(openNotice)).toBe("hello");
    expect(canSubmitAppeal(openNotice)).toBe(true);
    expect(getAppealStatusLabel(openNotice)).toBe("You can submit one appeal.");
  });

  it("reads category and reason without reporter identity", () => {
    expect(getOwnerReportCategorySummary(openNotice)).toBe("Spam");
    expect(getOwnerReportReasonSummary(openNotice)).toBe("Spammy");
    expect(getOwnerReportCategorySummary(openNotice)).not.toContain("sam");
    expect(getOwnerReportReasonSummary(openNotice)).not.toContain("sam@example.com");
  });

  it("reads the submitted owner appeal from AppealDTO", () => {
    expect(
      getOwnerAppealReason({
        ...openNotice,
        canAppeal: false,
        appealStatus: "OPEN",
        appeal: {
          appealId: 7,
          appealUser: { username: "alex" },
          appealReason: "This was a joke, please restore.",
        },
      }),
    ).toBe("This was a joke, please restore.");
    expect(
      getAppealUserName({
        ...openNotice,
        appeal: {
          appealUser: { username: "alex" },
          appealReason: "This was a joke, please restore.",
        },
      }),
    ).toBe("alex");
  });

  it("labels pending and decided states", () => {
    expect(
      getAppealStatusLabel({ canAppeal: false, appealStatus: "OPEN" }),
    ).toBe("Appeal pending review.");
    expect(
      getAppealStatusLabel({ canAppeal: false, appealStatus: "APPROVED" }),
    ).toBe("Appeal approved. Your content was restored.");
    expect(
      getAppealStatusLabel({ canAppeal: false, appealStatus: "DENIED" }),
    ).toBe("Appeal denied. This content stays removed.");
  });
});
