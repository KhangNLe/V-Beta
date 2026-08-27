import { API_BASE_URL } from "@/app/envExports";

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
 * Poll unread inbox items for the authenticated user.
 *
 * @param {import("firebase/auth").User} user
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchUnreadNotifications(user) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/notification/short`, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(
      await readErrorDetail(
        response,
        `Failed to load notifications: ${response.status}`,
      ),
    );
  }

  const payload = await response.json();
  return Array.isArray(payload) ? payload : [];
}

/**
 * Fetch all inbox items (read and unread).
 * <p>
 * The all-notifications endpoint is not wired yet. Until it exists this
 * returns the unread poll so the notifications page can render. Replace the
 * body of this function when the all-inbox API is available.
 *
 * @param {import("firebase/auth").User} user
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchAllNotifications(user) {
  return fetchUnreadNotifications(user);
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
