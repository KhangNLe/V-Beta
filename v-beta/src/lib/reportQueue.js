import { REPORT_CATEGORIES } from "@/api/reports";

/**
 * @param {unknown} payload
 * @returns {Array<Record<string, unknown>>}
 */
export function getQueueCases(payload) {
  if (!payload || typeof payload !== "object") return [];
  const reports = /** @type {Record<string, unknown>} */ (payload).reports;
  return Array.isArray(reports) ? reports : [];
}

/**
 * @param {unknown} queueCase
 * @returns {Record<string, unknown> | null}
 */
export function getCaseReport(queueCase) {
  if (!queueCase || typeof queueCase !== "object") return null;
  const report = /** @type {Record<string, unknown>} */ (queueCase).report;
  return report && typeof report === "object"
    ? /** @type {Record<string, unknown>} */ (report)
    : null;
}

/**
 * @param {unknown} queueCase
 * @returns {Array<Record<string, unknown>>}
 */
export function getCaseReporters(queueCase) {
  const report = getCaseReport(queueCase);
  if (!report || !Array.isArray(report.reporters)) return [];
  return report.reporters.filter((row) => row && typeof row === "object");
}

/**
 * @param {unknown} queueCase
 * @returns {number[]}
 */
export function getCaseReportIds(queueCase) {
  return getCaseReporters(queueCase)
    .map((row) => Number(row.reportId))
    .filter((id) => Number.isInteger(id) && id > 0);
}

/**
 * @param {unknown} queueCase
 * @returns {number | null}
 */
export function getCasePrimaryReportId(queueCase) {
  const ids = getCaseReportIds(queueCase);
  return ids.length > 0 ? ids[0] : null;
}

/**
 * @param {unknown} queueCase
 * @param {unknown} reportId
 * @returns {boolean}
 */
export function caseHasReportId(queueCase, reportId) {
  const parsed = Number(reportId);
  if (!Number.isInteger(parsed) || parsed <= 0) return false;
  return getCaseReportIds(queueCase).includes(parsed);
}

/**
 * @param {unknown} queueCase
 * @returns {string}
 */
export function getCaseTargetType(queueCase) {
  const report = getCaseReport(queueCase);
  return typeof report?.targetType === "string" ? report.targetType : "";
}

/**
 * @param {unknown} name
 * @returns {string}
 */
export function formatReportCategory(name) {
  if (typeof name !== "string" || !name) return "Uncategorized";
  const match = REPORT_CATEGORIES.find((category) => category.value === name);
  return match ? match.label : name.replaceAll("_", " ").toLowerCase();
}

/**
 * @param {unknown} queueCase
 * @returns {string}
 */
export function getCaseCategorySummary(queueCase) {
  const report = getCaseReport(queueCase);
  const tallies = Array.isArray(queueCase?.categories)
    ? queueCase.categories
    : [];
  if (tallies.length > 0) {
    return tallies
      .map((tally) => {
        const label = formatReportCategory(tally?.categoryName);
        const count = Number(tally?.reportCount) || 1;
        return count > 1 ? `${label} (${count})` : label;
      })
      .join(", ");
  }
  const names = [
    ...new Set(
      getCaseReporters(queueCase).map((row) =>
        formatReportCategory(row.categoryName),
      ),
    ),
  ];
  return names.join(", ") || formatReportCategory(report?.targetType);
}

/**
 * @param {unknown} queueCase
 * @returns {string}
 */
export function getCaseReporterSummary(queueCase) {
  const usernames = getCaseReporters(queueCase)
    .map((row) => {
      const reporter =
        row.reporter && typeof row.reporter === "object" ? row.reporter : {};
      return typeof reporter.username === "string" ? reporter.username : "";
    })
    .filter(Boolean);
  if (usernames.length === 0) return "Unknown reporter";
  if (usernames.length === 1) return usernames[0];
  return `${usernames[0]} +${usernames.length - 1}`;
}

/**
 * @param {unknown} queueCase
 * @returns {string | null}
 */
export function getCaseCreatedAt(queueCase) {
  const times = getCaseReporters(queueCase)
    .map((row) => row.createdAt)
    .filter((value) => typeof value === "string" || typeof value === "number")
    .map((value) => String(value));
  if (times.length === 0) return null;
  return times.sort().at(-1) ?? null;
}

/**
 * @param {string | null | undefined} raw
 * @returns {string}
 */
export function formatReportTime(raw) {
  if (!raw) return "Recently";
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) return String(raw);
  return parsed.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

/**
 * @param {unknown} queueCase
 * @returns {number}
 */
export function getCaseQueueScore(queueCase) {
  const score = Number(
    queueCase && typeof queueCase === "object" ? queueCase.queueScore : 0,
  );
  return Number.isFinite(score) ? score : 0;
}

/**
 * @param {unknown} url
 * @returns {string}
 */
export function inferVideoMimeType(url) {
  const value = String(url || "").toLowerCase();
  return value.endsWith(".webm") ? "video/webm" : "video/mp4";
}
