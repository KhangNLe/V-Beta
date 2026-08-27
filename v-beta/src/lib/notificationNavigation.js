/** Event types that land on the admin report queue. */
export const REPORT_QUEUE_EVENT_TYPES = new Set([
  "REPORT_CREATED",
  "REPORT_DISMISSED",
  "REPORT_APPROVED",
]);

/** Event types that land on deletion/appeal context. */
export const APPEAL_CONTEXT_EVENT_TYPES = new Set([
  "CONTENT_REMOVED",
  "APPEAL_SUBMITTED",
  "CONTENT_RESTORED",
  "APPEAL_DENIED",
]);

const EVENT_TYPE_LABELS = {
  REPORT_CREATED: "New report",
  REPORT_DISMISSED: "Report dismissed",
  REPORT_APPROVED: "Report approved",
  CONTENT_REMOVED: "Content removed",
  APPEAL_SUBMITTED: "Appeal submitted",
  CONTENT_RESTORED: "Content restored",
  APPEAL_DENIED: "Appeal denied",
};

/**
 * @param {unknown} notification
 * @returns {number | null}
 */
export function getNotificationId(notification) {
  if (!notification || typeof notification !== "object") return null;
  const parsed = Number(
    /** @type {Record<string, unknown>} */ (notification).notificationId,
  );
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * Unread when `readAt` is missing or null. Unread poll payloads omit `readAt`.
 * All-inbox rows from `GET /all` also omit `readAt`; overlay unread ids from
 * `/short` via {@link annotateInboxReadState} before rendering.
 *
 * @param {unknown} notification
 * @returns {boolean}
 */
export function isNotificationUnread(notification) {
  if (!notification || typeof notification !== "object") return true;
  const readAt = /** @type {Record<string, unknown>} */ (notification).readAt;
  return readAt == null || readAt === "";
}

/**
 * Marks all-inbox rows read/unread using ids from the unread poll.
 * `GET /api/notification/all` does not include `readAt`.
 *
 * @param {Array<Record<string, unknown>>} items
 * @param {Array<Record<string, unknown>>} unreadItems
 * @returns {Array<Record<string, unknown>>}
 */
export function annotateInboxReadState(items, unreadItems) {
  if (!Array.isArray(items)) return [];
  const unreadIds = new Set(
    (Array.isArray(unreadItems) ? unreadItems : [])
      .map(getNotificationId)
      .filter((id) => id != null),
  );
  return items.map((row) => {
    const id = getNotificationId(row);
    if (id != null && unreadIds.has(id)) {
      return { ...row, readAt: null };
    }
    if (
      row &&
      typeof row === "object" &&
      /** @type {Record<string, unknown>} */ (row).readAt != null &&
      /** @type {Record<string, unknown>} */ (row).readAt !== ""
    ) {
      return row;
    }
    return { ...row, readAt: "read" };
  });
}

/**
 * @param {string | null | undefined} raw
 * @returns {string}
 */
export function formatNotificationTime(raw) {
  if (!raw) return "Recently";
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) return String(raw);
  return parsed.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

/**
 * @param {unknown} notification
 * @returns {string | null}
 */
export function getNotificationCreatedAt(notification) {
  if (!notification || typeof notification !== "object") return null;
  const value = /** @type {Record<string, unknown>} */ (notification).createdAt;
  return typeof value === "string" || typeof value === "number" ? String(value) : null;
}

/**
 * @param {unknown} raw
 * @returns {number | null}
 */
function parsePositiveId(raw) {
  if (raw == null || raw === "") return null;
  const parsed = Number(raw);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/**
 * @param {unknown} notification
 * @returns {string}
 */
export function getNotificationEventType(notification) {
  if (!notification || typeof notification !== "object") return "";
  const summary = /** @type {Record<string, unknown>} */ (notification).summary;
  if (!summary || typeof summary !== "object") return "";
  const name = /** @type {Record<string, unknown>} */ (summary).eventTypeName;
  return typeof name === "string" ? name : "";
}

/**
 * @param {unknown} notification
 * @returns {string}
 */
export function getNotificationTypeLabel(notification) {
  const eventType = getNotificationEventType(notification);
  return EVENT_TYPE_LABELS[eventType] || eventType || "Notification";
}

/**
 * @param {unknown} notification
 * @returns {string}
 */
export function getNotificationDescription(notification) {
  if (!notification || typeof notification !== "object") return "";
  const summary = /** @type {Record<string, unknown>} */ (notification).summary;
  if (!summary || typeof summary !== "object") return "";
  const description = /** @type {Record<string, unknown>} */ (summary).description;
  return typeof description === "string" ? description : "";
}

/**
 * Builds the in-app path for an inbox item.
 * Click `kind` wins when wall/problem/account ids are present. Current
 * moderation events are `REPORT_QUEUE`; those split by event type so admins
 * open the reports queue and owners open appeal/deletion context.
 *
 * @param {unknown} notification
 * @returns {string}
 */
export function getNotificationHref(notification) {
  if (!notification || typeof notification !== "object") return "/notifications";
  const record = /** @type {Record<string, unknown>} */ (notification);
  const click =
    record.click && typeof record.click === "object"
      ? /** @type {Record<string, unknown>} */ (record.click)
      : {};
  const kind = typeof click.kind === "string" ? click.kind : "";
  const wallSectionId = parsePositiveId(click.wallSectionId);
  const problemId = parsePositiveId(click.problemId);
  const discussionId = parsePositiveId(click.discussionId);
  const reportId = parsePositiveId(click.reportId);
  const reportQuery = reportId ? `?reportId=${reportId}` : "";

  if (kind === "PROBLEM_DISCUSSION" && wallSectionId && problemId) {
    const discussionQuery = discussionId ? `?discussionId=${discussionId}` : "";
    return `/wall/${wallSectionId}/problem/${problemId}${discussionQuery}`;
  }
  if (kind === "PROBLEM" && wallSectionId && problemId) {
    return `/wall/${wallSectionId}/problem/${problemId}`;
  }
  if (kind === "WALL_SECTION" && wallSectionId) {
    return `/wall/${wallSectionId}`;
  }
  if (kind === "ACCOUNT") {
    return "/account";
  }

  const eventType = getNotificationEventType(notification);
  if (APPEAL_CONTEXT_EVENT_TYPES.has(eventType)) {
    return `/appeals${reportQuery}`;
  }
  if (REPORT_QUEUE_EVENT_TYPES.has(eventType) || kind === "REPORT_QUEUE") {
    return `/reports${reportQuery}`;
  }
  return "/notifications";
}
