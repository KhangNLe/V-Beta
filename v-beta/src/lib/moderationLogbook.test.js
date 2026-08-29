import {
  buildLogbookTxt,
  formatLogbookDecision,
  getLogbookActorName,
  getLogbookProblemHref,
  getLogbookReportHref,
  getLogbookReportId,
} from "./moderationLogbook";

const dismissed = {
  moderationId: 40,
  decision: "REPORT_DISMISSED",
  adminNote: "Does not violate gym guidelines.",
  createdAt: "2026-08-18T18:05:00Z",
  resolvedBy: { userId: 3, username: "testAdmin", role: "ADMIN" },
  report: {
    targetType: "DISCUSSION",
    climbingProblem: { problemId: 100, holdColor: "Red" },
    wallSection: { wallSectionID: 10, wallSectionName: "Cave" },
    reporters: [{ reportId: 11, reporter: { username: "sam" } }],
  },
};

const appealDenied = {
  ...dismissed,
  moderationId: 41,
  decision: "APPEAL_DENIED",
};

describe("moderationLogbook helpers", () => {
  it("labels decisions and reads report/actor", () => {
    expect(formatLogbookDecision("REPORT_DISMISSED")).toBe("Dismissed");
    expect(formatLogbookDecision("CONTENT_REMOVED")).toBe("Content removed");
    expect(formatLogbookDecision("APPEAL_APPROVED")).toBe("Appeal approved");
    expect(getLogbookActorName(dismissed)).toBe("testAdmin");
    expect(getLogbookReportId(dismissed)).toBe(11);
  });

  it("links to report detail or appeals when a report id exists", () => {
    expect(getLogbookReportHref(dismissed)).toBe("/reports?reportId=11");
    expect(getLogbookReportHref(appealDenied)).toBe("/appeals?reportId=11");
    expect(getLogbookProblemHref(dismissed)).toBe("/wall/10/problem/100");
  });

  it("formats a downloadable txt log", () => {
    const text = buildLogbookTxt([dismissed], {
      exportedAt: new Date("2026-08-28T12:00:00.000Z"),
    });
    expect(text).toContain("V-Beta moderation logbook");
    expect(text).toContain("Exported: 2026-08-28T12:00:00.000Z");
    expect(text).toContain("Moderation ID: 40");
    expect(text).toContain("Decision: Dismissed (REPORT_DISMISSED)");
    expect(text).toContain("Admin: testAdmin");
    expect(text).toContain("Report ID: 11");
    expect(text).toContain("Notes: Does not violate gym guidelines.");
  });
});
