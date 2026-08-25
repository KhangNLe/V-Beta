import { API_BASE_URL } from "@/app/envExports";

export const REPORT_CATEGORIES = [
  { value: "INAPPROPRIATE_CONTENT", label: "Inappropriate content" },
  { value: "HARASSMENT_BULLYING", label: "Harassment or bullying" },
  { value: "SPAM", label: "Spam" },
  { value: "OFF_TOPIC", label: "Off-topic" },
];

export const REPORT_REASON_MAX_LENGTH = 250;

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
