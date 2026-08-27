import { API_BASE_URL } from "@/app/envExports";
import { annotateInboxReadState } from "@/lib/notificationNavigation";

/** Page size for `GET /api/notification/all` (1-based `offset`). */
export const ALL_NOTIFICATIONS_PAGE_SIZE = 10;

/**
 * @param {Response} response
 * @param {string} fallback
 */
async function readErrorDetail(response, fallback) {
  let detail = fallback;
  try {
    const text = await response.text();
    if (text && text.trim()) {
      detail = text.trim().slice(0, 200);
    }
  } catch {
    // ignore body parse failures
  }
  return detail;
}

/**
 * @param {import("firebase/auth").User} user
 * @param {string} pathWithQuery
 * @param {string} fallback
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
async function fetchNotificationArray(user, pathWithQuery, fallback) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}${pathWithQuery}`, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(
      await readErrorDetail(response, `${fallback}: ${response.status}`),
    );
  }

  const payload = await response.json();
  return Array.isArray(payload) ? payload : [];
}

/**
 * @param {unknown} offset
 * @returns {number}
 */
function normalizeAllInboxOffset(offset) {
  const parsed = Number(offset);
  return Number.isInteger(parsed) && parsed >= 1 ? parsed : 1;
}

/**
 * Poll unread inbox items for the authenticated user.
 *
 * @param {import("firebase/auth").User} user
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchUnreadNotifications(user) {
  return fetchNotificationArray(
    user,
    "/api/notification/short",
    "Failed to load notifications",
  );
}

/**
 * Fetch one page of inbox items (read and unread).
 *
 * Calls `GET /api/notification/all?offset=` (1-based, 10 rows). The payload
 * omits `readAt`, so unread ids from `GET /short` are overlaid when that
 * poll succeeds.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} [offset=1]
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchAllNotifications(user, offset = 1) {
  const page = normalizeAllInboxOffset(offset);
  const [pageResult, unreadResult] = await Promise.allSettled([
    fetchNotificationArray(
      user,
      `/api/notification/all?offset=${encodeURIComponent(String(page))}`,
      "Failed to load notifications",
    ),
    fetchUnreadNotifications(user),
  ]);

  if (pageResult.status === "rejected") {
    throw pageResult.reason;
  }

  const items = pageResult.value;
  if (unreadResult.status !== "fulfilled") {
    return items;
  }
  return annotateInboxReadState(items, unreadResult.value);
}

/**
 * Mark one of the caller's notifications as read.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} notificationId
 */
export async function markNotificationRead(user, notificationId) {
  const idToken = await user.getIdToken();
  const response = await fetch(
    `${API_BASE_URL}/api/notification/short?notificationId=${encodeURIComponent(String(notificationId))}`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    },
  );

  if (!response.ok) {
    throw new Error(
      await readErrorDetail(
        response,
        `Failed to mark notification read: ${response.status}`,
      ),
    );
  }
}
