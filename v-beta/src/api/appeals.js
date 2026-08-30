import { API_BASE_URL } from "@/app/envExports";

/** Owner appeal reason on `POST /api/moderate/appeal`. */
export const APPEAL_REASON_MAX_LENGTH = 250;

/** Admin comments on `PATCH /api/moderate/appeal` (`ModerateAppealRequest.adminReason`). */
export const ADMIN_REASON_MAX_LENGTH = 255;

export const APPEAL_RESOLVE_STATUSES = {
  APPROVED: "APPROVED",
  DENIED: "DENIED",
};

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
 * Load the owner deletion notice for one report.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} reportId
 * @returns {Promise<Record<string, unknown>>}
 */
export async function fetchDeletionNotice(user, reportId) {
  const idToken = await user.getIdToken();
  const response = await fetch(
    `${API_BASE_URL}/api/moderate/appeal/notice?reportId=${encodeURIComponent(String(reportId))}`,
    {
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    },
  );

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, "Appeal is not allowed"),
    );
  }

  return response.json();
}

/**
 * Load OPEN appeals for the admin queue, or one appeal by id or report id.
 *
 * @param {import("firebase/auth").User} user
 * @param {{ appealId?: number, reportId?: number }} [query]
 * @returns {Promise<{ appeals: Array<Record<string, unknown>> }>}
 */
export async function fetchAppeals(user, query = {}) {
  const idToken = await user.getIdToken();
  const params = new URLSearchParams();
  if (query.appealId != null) {
    params.set("appealId", String(query.appealId));
  } else if (query.reportId != null) {
    params.set("reportId", String(query.reportId));
  }
  const suffix = params.toString() ? `?${params.toString()}` : "";
  const response = await fetch(`${API_BASE_URL}/api/moderate/appeal${suffix}`, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });

  if (!response.ok) {
    if (query.appealId != null || query.reportId != null) {
      if (response.status === 404) {
        throw new Error(await readErrorDetail(response, "Appeal not found"));
      }
    }
    if (response.status === 401 || response.status === 403 || response.status === 404) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, `Failed to load appeals: ${response.status}`),
    );
  }

  const payload = await response.json();
  const appeals = payload && Array.isArray(payload.appeals) ? payload.appeals : [];
  return { appeals };
}

/**
 * Approve or deny one OPEN appeal.
 *
 * Body matches {@code ModerateAppealRequest}: {@code appealId},
 * {@code appealStatus} ({@code APPROVED} or {@code DENIED}), and
 * {@code adminReason} (required, max 255).
 *
 * @param {import("firebase/auth").User} user
 * @param {{
 *   appealId: number,
 *   appealStatus: string,
 *   adminReason: string,
 * }} payload
 */
export async function resolveAppeal(user, payload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/moderate/appeal`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      appealId: payload.appealId,
      appealStatus: payload.appealStatus,
      adminReason: payload.adminReason,
    }),
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403 || response.status === 404) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, `Failed to resolve appeal: ${response.status}`),
    );
  }
}

/**
 * Submit a one-time appeal for a {@code CONTENT_REMOVED} report the caller owns.
 *
 * @param {import("firebase/auth").User} user
 * @param {{ reportId: number, appealReason: string }} payload
 */
export async function createAppeal(user, payload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/moderate/appeal`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      reportId: payload.reportId,
      appealReason: payload.appealReason,
    }),
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, `Failed to submit appeal: ${response.status}`),
    );
  }
}
