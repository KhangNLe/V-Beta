import {
  appealHasReportId,
  getAppealContentSummary,
  getAppealFlags,
  getAppealQueue,
  getAppealReason,
  getAppealReportId,
  getAppealUserName,
} from "./appealQueue";

const appeal = {
  appealId: 7,
  appealUser: { username: "alex" },
  appealReason: "This was a joke, please restore.",
  report: {
    discussion: {
      discussionType: "COMMENT",
      discussionContent: "hello",
    },
    reporters: [
      {
        reportId: 11,
        reporter: { username: "sam" },
        categoryName: "SPAM",
        reportReason: "Spammy",
      },
    ],
  },
};

describe("appealQueue helpers", () => {
  it("reads AppealDTO list fields including reporter identity", () => {
    expect(getAppealQueue({ appeals: [appeal] })).toHaveLength(1);
    expect(getAppealUserName(appeal)).toBe("alex");
    expect(getAppealReason(appeal)).toBe("This was a joke, please restore.");
    expect(getAppealContentSummary(appeal)).toBe("hello");
    expect(getAppealReportId(appeal)).toBe(11);
    expect(appealHasReportId(appeal, 11)).toBe(true);
    expect(getAppealFlags(appeal)).toEqual([
      { category: "Spam", reason: "Spammy", reporter: "sam" },
    ]);
  });
});
