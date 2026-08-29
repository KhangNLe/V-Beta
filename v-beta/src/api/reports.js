import { API_BASE_URL } from "@/app/envExports";

export const REPORT_CATEGORIES = [
  { value: "INAPPROPRIATE_CONTENT", label: "Inappropriate content" },
  { value: "HARASSMENT_BULLYING", label: "Harassment or bullying" },
  { value: "SPAM", label: "Spam" },
  { value: "OFF_TOPIC", label: "Off-topic" },
];

export const REPORT_REASON_MAX_LENGTH = 250;

/** Admin notes on `POST /api/moderate/report` (`Moderation_Action.admin_notes`). */
export const ADMIN_NOTES_MAX_LENGTH = 255;

export const REPORT_RESOLVE_DECISIONS = {
  DISMISS: "REPORT_DISMISSED",
  REMOVE: "CONTENT_REMOVED",
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
 * Submit a content report for the authenticated user.
 *
 * @param {import("firebase/auth").User} user
 * @param {{
 *   reportTargetType: string,
 *   reportReason: string,
 *   reportCategoryName: string,
 *   targetId: number,
 * }} payload
 */
export async function createContentReport(user, payload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/report/create`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      reportTargetType: payload.reportTargetType,
      reportReason: payload.reportReason,
      reportCategoryName: payload.reportCategoryName,
      targetId: payload.targetId,
    }),
  });

  if (!response.ok) {
    let detail = `Failed to submit report: ${response.status}`;
    try {
      const text = await response.text();
      if (text && text.trim()) {
        detail = text.trim().slice(0, 200);
      }
    } catch {
      // ignore body parse failures
    }
    throw new Error(detail);
  }
}

/**
 * @param {import("firebase/auth").User} user
 * @param {number} [reportId]
 * @returns {Promise<{ reports: Array<Record<string, unknown>> }>}
 */
export async function fetchReportQueue(user, reportId) {
  const idToken = await user.getIdToken();
  const query =
    reportId == null
      ? ""
      : `?reportId=${encodeURIComponent(String(reportId))}`;
  const response = await fetch(`${API_BASE_URL}/api/report/reports${query}`, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });

  if (!response.ok) {
    if (reportId != null && response.status === 404) {
      throw new Error(
        await readErrorDetail(response, "Report not found"),
      );
    }
    if (response.status === 401 || response.status === 403 || response.status === 404) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, `Failed to load reports: ${response.status}`),
    );
  }

  const payload = await response.json();
  const reports = payload && Array.isArray(payload.reports) ? payload.reports : [];
  return { reports };
}

/**
 * Close OPEN reporter rows on a discussion case.
 *
 * @param {import("firebase/auth").User} user
 * @param {{
 *   reportIds: number[],
 *   decision: string,
 *   reason: string,
 * }} payload
 */
export async function resolveReports(user, payload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/moderate/report`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      reportIds: payload.reportIds,
      decision: payload.decision,
      reason: payload.reason,
    }),
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403 || response.status === 404) {
      throw new Error("Access denied.");
    }
    throw new Error(
      await readErrorDetail(response, `Failed to resolve report: ${response.status}`),
    );
  }
}
