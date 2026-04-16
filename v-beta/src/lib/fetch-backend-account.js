import { API_BASE_URL } from "@/app/envExports"

/**
 * GET /api/account for the current Firebase user. Throws on non-OK or network error.
 *
 * @param {string} idToken
 * @returns {Promise<{ userId?: number, username?: string, email?: string, role?: string | null }>}
 */
export async function fetchBackendAccount(idToken) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}/api/account`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : "Failed to reach backend."
    throw new Error(`Backend account fetch failed: ${message}`)
  }

  if (!response.ok) {
    let detail = ""
    try {
      const text = await response.text()
      detail = text ? `: ${text.slice(0, 200)}` : ""
    } catch {
      // ignore
    }
    throw new Error(`Backend account failed (${response.status} ${response.statusText})${detail}`)
  }

  const text = await response.text()
  if (!text.trim()) {
    return {}
  }
  try {
    return JSON.parse(text)
  } catch {
    return {}
  }
}
