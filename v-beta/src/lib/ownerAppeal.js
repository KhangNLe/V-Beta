import { formatReportCategory } from "./reportQueue";

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getDeletionAdminReason(notice) {
  if (!notice || typeof notice !== "object") return "";
  const reason = /** @type {Record<string, unknown>} */ (notice).adminReason;
  return typeof reason === "string" ? reason : "";
}

/**
 * @param {unknown} notice
 * @returns {boolean}
 */
export function canSubmitAppeal(notice) {
  if (!notice || typeof notice !== "object") return false;
  return /** @type {Record<string, unknown>} */ (notice).canAppeal === true;
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getAppealStatusLabel(notice) {
  if (!notice || typeof notice !== "object") return "";
  const record = /** @type {Record<string, unknown>} */ (notice);
  if (record.canAppeal === true) return "You can submit one appeal.";
  const appealStatus = record.appealStatus;
  if (appealStatus === "OPEN") return "Appeal pending review.";
  if (appealStatus === "APPROVED" || record.reportStatus === "CONTENT_RESTORED") {
    return "Appeal approved. Your content was restored.";
  }
  if (appealStatus === "DENIED" || record.reportStatus === "APPEAL_DENIED") {
    return "Appeal denied. This content stays removed.";
  }
  if (record.reportStatus === "APPEAL_PENDING") return "Appeal pending review.";
  return "This removal cannot be appealed again.";
}

/**
 * Category and reason rows from the owner notice. Reporter identity is ignored.
 *
 * @param {unknown} notice
 * @returns {Array<{ category: string, reason: string }>}
 */
export function getOwnerReportFlags(notice) {
  if (!notice || typeof notice !== "object") return [];
  const report = /** @type {Record<string, unknown>} */ (notice).report;
  if (!report || typeof report !== "object" || !Array.isArray(report.reporters)) {
    return [];
  }
  return report.reporters
    .filter((row) => row && typeof row === "object")
    .map((row) => ({
      category: formatReportCategory(row.categoryName),
      reason:
        typeof row.reportReason === "string" ? row.reportReason.trim() : "",
    }));
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getOwnerReportCategorySummary(notice) {
  const labels = [
    ...new Set(
      getOwnerReportFlags(notice)
        .map((flag) => flag.category)
        .filter(Boolean),
    ),
  ];
  return labels.join(", ");
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getOwnerReportReasonSummary(notice) {
  return [
    ...new Set(
      getOwnerReportFlags(notice)
        .map((flag) => flag.reason)
        .filter(Boolean),
    ),
  ].join("\n");
}

/**
 * Nested {@code AppealDTO} on the deletion notice, when an appeal exists.
 *
 * @param {unknown} notice
 * @returns {Record<string, unknown> | null}
 */
export function getNoticeAppeal(notice) {
  if (!notice || typeof notice !== "object") return null;
  const appeal = /** @type {Record<string, unknown>} */ (notice).appeal;
  return appeal && typeof appeal === "object"
    ? /** @type {Record<string, unknown>} */ (appeal)
    : null;
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getOwnerAppealReason(notice) {
  const appeal = getNoticeAppeal(notice);
  if (!appeal) return "";
  const reason = appeal.appealReason;
  return typeof reason === "string" ? reason.trim() : "";
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getAppealUserName(notice) {
  const appeal = getNoticeAppeal(notice);
  const user = appeal?.appealUser;
  if (!user || typeof user !== "object") return "";
  const username = /** @type {Record<string, unknown>} */ (user).username;
  return typeof username === "string" ? username.trim() : "";
}

/**
 * @param {unknown} notice
 * @returns {string}
 */
export function getRemovedContentSummary(notice) {
  if (!notice || typeof notice !== "object") return "";
  const report = /** @type {Record<string, unknown>} */ (notice).report;
  if (!report || typeof report !== "object") return "";
  const discussion = report.discussion;
  if (!discussion || typeof discussion !== "object") return "";
  const kind =
    typeof discussion.discussionType === "string"
      ? discussion.discussionType
      : "DISCUSSION";
  const content =
    typeof discussion.discussionContent === "string"
      ? discussion.discussionContent
      : "";
  if (kind === "BETA" && content) return `Solution beta: ${content}`;
  if (content) return content;
  return kind === "BETA" ? "Solution beta" : "Comment";
}
