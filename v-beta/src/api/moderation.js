import { API_BASE_URL } from "@/app/envExports";

/** Page size for `GET /api/moderate/logbook` (1-based `offSetPlace`). */
export const LOGBOOK_PAGE_SIZE = 25;

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
 * @param {unknown} offSetPlace
 * @returns {number}
 */
function normalizeOffSetPlace(offSetPlace) {
  const parsed = Number(offSetPlace);
  return Number.isInteger(parsed) && parsed >= 1 ? parsed : 1;
}

/**
 * Read append-only moderation logbook rows.
 *
 * @param {import("firebase/auth").User} user
 * @param {{ offSetPlace?: number, moderationId?: number }} [options]
 * @returns {Promise<{ moderationLogs: Array<Record<string, unknown>> }>}
 */
export async function fetchLogbook(user, options = {}) {
  const idToken = await user.getIdToken();
  const params = new URLSearchParams();
  if (options.moderationId != null) {
    params.set("moderationId", String(options.moderationId));
  } else if (options.offSetPlace != null) {
    params.set("offSetPlace", String(normalizeOffSetPlace(options.offSetPlace)));
  }
  const query = params.toString() ? `?${params.toString()}` : "";
  const response = await fetch(`${API_BASE_URL}/api/moderate/logbook${query}`, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });

  if (!response.ok) {
    if (options.moderationId != null && response.status === 404) {
      throw new Error(
        await readErrorDetail(response, "Moderation not found"),
      );
    }
    if (
      response.status === 401 ||
      response.status === 403 ||
      response.status === 404
    ) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(
        response,
        `Failed to load logbook: ${response.status}`,
      ),
    );
  }

  const payload = await response.json();
  const moderationLogs =
    payload && Array.isArray(payload.moderationLogs)
      ? payload.moderationLogs
      : [];
  return { moderationLogs };
}

/**
 * Walk 1-based pages until a short page (full export for .txt download).
 *
 * @param {import("firebase/auth").User} user
 * @returns {Promise<Array<Record<string, unknown>>>}
 */
export async function fetchAllLogbookEntries(user) {
  const entries = [];
  let page = 1;
  for (;;) {
    const { moderationLogs } = await fetchLogbook(user, {
      offSetPlace: page,
    });
    entries.push(...moderationLogs);
    if (moderationLogs.length < LOGBOOK_PAGE_SIZE) break;
    page += 1;
    if (page > 200) break;
  }
  return entries;
}
