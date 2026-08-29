import { formatReportTime } from "@/lib/reportQueue";

export const LOGBOOK_DECISION_LABELS = {
  REPORT_DISMISSED: "Dismissed",
  CONTENT_REMOVED: "Content removed",
  APPEAL_APPROVED: "Appeal approved",
  APPEAL_DENIED: "Appeal denied",
};

/**
 * @param {unknown} payload
 * @returns {Array<Record<string, unknown>>}
 */
export function getLogbookEntries(payload) {
  if (!payload || typeof payload !== "object") return [];
  const logs = /** @type {Record<string, unknown>} */ (payload).moderationLogs;
  return Array.isArray(logs) ? logs : [];
}

/**
 * @param {unknown} entry
 * @returns {number | null}
 */
export function getLogbookId(entry) {
  const parsed = Number(
    entry && typeof entry === "object" ? entry.moderationId : NaN,
  );
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * @param {unknown} entry
 * @returns {Record<string, unknown> | null}
 */
export function getLogbookReport(entry) {
  if (!entry || typeof entry !== "object") return null;
  const report = /** @type {Record<string, unknown>} */ (entry).report;
  return report && typeof report === "object"
    ? /** @type {Record<string, unknown>} */ (report)
    : null;
}

/**
 * @param {unknown} entry
 * @returns {number | null}
 */
export function getLogbookReportId(entry) {
  const report = getLogbookReport(entry);
  const reporters = Array.isArray(report?.reporters) ? report.reporters : [];
  const first = reporters[0];
  const parsed = Number(
    first && typeof first === "object" ? first.reportId : NaN,
  );
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * @param {unknown} decision
 * @returns {string}
 */
export function formatLogbookDecision(decision) {
  if (typeof decision !== "string" || !decision) return "Unknown decision";
  return LOGBOOK_DECISION_LABELS[decision] || decision.replaceAll("_", " ");
}

/**
 * @param {unknown} entry
 * @returns {string}
 */
export function getLogbookActorName(entry) {
  if (!entry || typeof entry !== "object") return "Unknown admin";
  const resolvedBy = /** @type {Record<string, unknown>} */ (entry).resolvedBy;
  if (!resolvedBy || typeof resolvedBy !== "object") return "Unknown admin";
  const username = /** @type {Record<string, unknown>} */ (resolvedBy).username;
  return typeof username === "string" && username ? username : "Unknown admin";
}

/**
 * @param {unknown} entry
 * @returns {string}
 */
export function getLogbookNotes(entry) {
  if (!entry || typeof entry !== "object") return "";
  const note = /** @type {Record<string, unknown>} */ (entry).adminNote;
  return typeof note === "string" ? note : "";
}

/**
 * Report-queue or appeals landing for this decided row, when a report id exists.
 *
 * @param {unknown} entry
 * @returns {string | null}
 */
export function getLogbookReportHref(entry) {
  const reportId = getLogbookReportId(entry);
  if (!reportId) return null;
  const decision =
    entry && typeof entry === "object" && typeof entry.decision === "string"
      ? entry.decision
      : "";
  if (decision === "APPEAL_APPROVED" || decision === "APPEAL_DENIED") {
    return `/appeals?reportId=${reportId}`;
  }
  return `/reports?reportId=${reportId}`;
}

/**
 * Problem page for discussion context when wall/problem snapshots are present.
 *
 * @param {unknown} entry
 * @returns {string | null}
 */
export function getLogbookProblemHref(entry) {
  const report = getLogbookReport(entry);
  const wall =
    report?.wallSection && typeof report.wallSection === "object"
      ? report.wallSection
      : null;
  const problem =
    report?.climbingProblem && typeof report.climbingProblem === "object"
      ? report.climbingProblem
      : null;
  const wallId = Number(wall?.wallSectionID);
  const problemId = Number(problem?.problemId);
  if (
    !Number.isInteger(wallId) ||
    wallId <= 0 ||
    !Number.isInteger(problemId) ||
    problemId <= 0
  ) {
    return null;
  }
  return `/wall/${wallId}/problem/${problemId}`;
}

/**
 * @param {Array<Record<string, unknown>>} entries
 * @param {{ exportedAt?: Date }} [options]
 * @returns {string}
 */
export function buildLogbookTxt(entries, options = {}) {
  const exportedAt = options.exportedAt ?? new Date();
  const header = [
    "V-Beta moderation logbook",
    `Exported: ${exportedAt.toISOString()}`,
    `Entries: ${entries.length}`,
    "",
  ];
  const blocks = entries.map((entry) => {
    const report = getLogbookReport(entry);
    const targetType =
      typeof report?.targetType === "string" ? report.targetType : "";
    return [
      "---",
      `Moderation ID: ${getLogbookId(entry) ?? ""}`,
      `Decision: ${formatLogbookDecision(entry.decision)} (${typeof entry.decision === "string" ? entry.decision : ""})`,
      `Admin: ${getLogbookActorName(entry)}`,
      `Time: ${formatReportTime(typeof entry.createdAt === "string" || typeof entry.createdAt === "number" ? String(entry.createdAt) : null)}`,
      `Report ID: ${getLogbookReportId(entry) ?? ""}`,
      `Target: ${targetType}`,
      `Notes: ${getLogbookNotes(entry)}`,
      "",
    ].join("\n");
  });
  return `${header.join("\n")}${blocks.join("\n")}`.trimEnd() + "\n";
}

/**
 * Trigger a browser download of a UTF-8 text file.
 *
 * @param {string} filename
 * @param {string} text
 */
export function downloadTextFile(filename, text) {
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = "noopener";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
