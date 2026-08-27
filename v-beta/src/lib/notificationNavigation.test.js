import {
  getNotificationHref,
  getNotificationTypeLabel,
  isNotificationUnread,
} from "./notificationNavigation";

const reportCreated = {
  notificationId: 81,
  summary: {
    eventTypeName: "REPORT_CREATED",
    description: "A user submitted a content report",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
    wallSectionId: null,
    problemId: null,
    discussionId: null,
    userId: null,
  },
};

const contentRemoved = {
  notificationId: 83,
  summary: {
    eventTypeName: "CONTENT_REMOVED",
    description: "One of your content had been reported and removed.",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
    wallSectionId: null,
    problemId: null,
    discussionId: null,
    userId: null,
  },
};

describe("notificationNavigation", () => {
  it("sends admin new-report clicks to the reports queue", () => {
    expect(getNotificationHref(reportCreated)).toBe("/reports?reportId=11");
    expect(getNotificationTypeLabel(reportCreated)).toBe("New report");
  });

  it("sends reporter outcomes to the reports queue", () => {
    expect(
      getNotificationHref({
        ...reportCreated,
        summary: { eventTypeName: "REPORT_DISMISSED", description: "Dismissed" },
      }),
    ).toBe("/reports?reportId=11");
    expect(
      getNotificationHref({
        ...reportCreated,
        summary: { eventTypeName: "REPORT_APPROVED", description: "Approved" },
      }),
    ).toBe("/reports?reportId=11");
  });

  it("sends owner deletion and appeal events to appeal context", () => {
    expect(getNotificationHref(contentRemoved)).toBe("/appeals?reportId=11");
    expect(
      getNotificationHref({
        ...contentRemoved,
        summary: { eventTypeName: "APPEAL_SUBMITTED", description: "Appeal" },
      }),
    ).toBe("/appeals?reportId=11");
    expect(
      getNotificationHref({
        ...contentRemoved,
        summary: { eventTypeName: "CONTENT_RESTORED", description: "Restored" },
      }),
    ).toBe("/appeals?reportId=11");
    expect(
      getNotificationHref({
        ...contentRemoved,
        summary: { eventTypeName: "APPEAL_DENIED", description: "Denied" },
      }),
    ).toBe("/appeals?reportId=11");
  });

  it("uses problem and wall click kinds when ids are present", () => {
    expect(
      getNotificationHref({
        click: {
          kind: "PROBLEM_DISCUSSION",
          wallSectionId: 10,
          problemId: 100,
          discussionId: 301,
        },
      }),
    ).toBe("/wall/10/problem/100?discussionId=301");
    expect(
      getNotificationHref({
        click: { kind: "WALL_SECTION", wallSectionId: 10 },
      }),
    ).toBe("/wall/10");
  });

  it("treats missing readAt as unread", () => {
    expect(isNotificationUnread(reportCreated)).toBe(true);
    expect(isNotificationUnread({ ...reportCreated, readAt: "2026-08-14T20:00:00Z" })).toBe(false);
  });
});
