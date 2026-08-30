import { formatReportCategory } from "./reportQueue";
import { getRemovedContentSummary } from "./ownerAppeal";

/**
 * @param {unknown} payload
 * @returns {Array<Record<string, unknown>>}
 */
export function getAppealQueue(payload) {
  if (!payload || typeof payload !== "object") return [];
  const appeals = /** @type {Record<string, unknown>} */ (payload).appeals;
  return Array.isArray(appeals)
    ? appeals.filter((row) => row && typeof row === "object")
    : [];
}

/**
 * @param {unknown} appeal
 * @returns {number | null}
 */
export function getAppealId(appeal) {
  const parsed = Number(
    appeal && typeof appeal === "object" ? appeal.appealId : NaN,
  );
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * @param {unknown} appeal
 * @returns {Record<string, unknown> | null}
 */
export function getAppealReport(appeal) {
  if (!appeal || typeof appeal !== "object") return null;
  const report = /** @type {Record<string, unknown>} */ (appeal).report;
  return report && typeof report === "object"
    ? /** @type {Record<string, unknown>} */ (report)
    : null;
}

/**
 * @param {unknown} appeal
 * @returns {number | null}
 */
export function getAppealReportId(appeal) {
  const report = getAppealReport(appeal);
  const reporters = Array.isArray(report?.reporters) ? report.reporters : [];
  const first = reporters[0];
  const parsed = Number(
    first && typeof first === "object" ? first.reportId : NaN,
  );
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * @param {unknown} appeal
 * @param {unknown} reportId
 * @returns {boolean}
 */
export function appealHasReportId(appeal, reportId) {
  const parsed = Number(reportId);
  if (!Number.isInteger(parsed) || parsed <= 0) return false;
  return getAppealReportId(appeal) === parsed;
}

/**
 * @param {unknown} appeal
 * @returns {string}
 */
export function getAppealUserName(appeal) {
  if (!appeal || typeof appeal !== "object") return "";
  const user = /** @type {Record<string, unknown>} */ (appeal).appealUser;
  if (!user || typeof user !== "object") return "";
  const username = /** @type {Record<string, unknown>} */ (user).username;
  return typeof username === "string" ? username.trim() : "";
}

/**
 * @param {unknown} appeal
 * @returns {string}
 */
export function getAppealReason(appeal) {
  if (!appeal || typeof appeal !== "object") return "";
  const reason = /** @type {Record<string, unknown>} */ (appeal).appealReason;
  return typeof reason === "string" ? reason.trim() : "";
}

/**
 * @param {unknown} appeal
 * @returns {string}
 */
export function getAppealContentSummary(appeal) {
  return getRemovedContentSummary({ report: getAppealReport(appeal) });
}

/**
 * @param {unknown} appeal
 * @returns {Array<{ category: string, reason: string, reporter: string }>}
 */
export function getAppealFlags(appeal) {
  const report = getAppealReport(appeal);
  const reporters = Array.isArray(report?.reporters) ? report.reporters : [];
  return reporters
    .filter((row) => row && typeof row === "object")
    .map((row) => {
      const reporter =
        row.reporter && typeof row.reporter === "object" ? row.reporter : {};
      return {
        category: formatReportCategory(row.categoryName),
        reason:
          typeof row.reportReason === "string" ? row.reportReason.trim() : "",
        reporter:
          typeof reporter.username === "string" ? reporter.username.trim() : "",
      };
    });
}
