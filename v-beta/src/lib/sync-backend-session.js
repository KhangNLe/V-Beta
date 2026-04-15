import { auth } from "@/app/firebase"
import { API_BASE_URL } from "@/app/envExports"

/**
 * POST session to the app backend. Throws if the request fails or returns non-OK.
 *
 * @param {object} [options]
 * @param {string} [options.username] - Preferred username (e.g. from signup "Full name"). If missing/empty, uses Firebase displayName, then email local part, then "user".
 */
export async function syncSessionWithBackend(options = {}) {
  const currentUser = auth.currentUser
  if (!currentUser) {
    throw new Error("No authenticated user found.")
  }

  const explicit = options.username
  const username =
    typeof explicit === "string" && explicit.trim() !== ""
      ? explicit.trim()
      : currentUser.displayName?.trim() ||
        currentUser.email?.split("@")[0] ||
        "user"

  const idToken = await currentUser.getIdToken()
  let response
  try {
    response = await fetch(`${API_BASE_URL}/api/accounts/session`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Authorization: `Bearer ${idToken}`,
      },
      body: JSON.stringify({
        username,
        email: currentUser.email || "",
      }),
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : "Failed to reach backend."
    throw new Error(`Backend session sync failed: ${message}`)
  }

  if (!response.ok) {
    let detail = ""
    try {
      const text = await response.text()
      detail = text ? `: ${text.slice(0, 200)}` : ""
    } catch {
      // ignore
    }
    throw new Error(`Backend session failed (${response.status} ${response.statusText})${detail}`)
  }
}
