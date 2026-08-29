import {
  caseHasReportId,
  formatReportCategory,
  getCaseCategorySummary,
  getCasePrimaryReportId,
  getCaseQueueScore,
  getCaseReporterSummary,
  getQueueCases,
} from "./reportQueue";

const spamCase = {
  queueScore: 4,
  categories: [{ categoryName: "SPAM", reportCount: 2, categoryScore: 4 }],
  report: {
    targetType: "DISCUSSION",
    reporters: [
      { reportId: 11, reporter: { username: "sam" }, categoryName: "SPAM" },
      { reportId: 12, reporter: { username: "lee" }, categoryName: "SPAM" },
    ],
  },
};

describe("reportQueue helpers", () => {
  it("reads ranked cases from the payload", () => {
    expect(getQueueCases({ reports: [spamCase] })).toEqual([spamCase]);
    expect(getQueueCases({})).toEqual([]);
  });

  it("summarizes reporters, categories, and score", () => {
    expect(formatReportCategory("SPAM")).toBe("Spam");
    expect(getCaseCategorySummary(spamCase)).toBe("Spam (2)");
    expect(getCaseReporterSummary(spamCase)).toBe("sam +1");
    expect(getCaseQueueScore(spamCase)).toBe(4);
    expect(getCasePrimaryReportId(spamCase)).toBe(11);
    expect(caseHasReportId(spamCase, 12)).toBe(true);
    expect(caseHasReportId(spamCase, 99)).toBe(false);
  });
});
