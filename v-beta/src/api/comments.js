import { API_BASE_URL } from "@/app/envExports";

/**
 * Post a new comment for a problem.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} problemId
 * @param {string} commentInfo
 */
export async function postCommentForUser(user, problemId, commentInfo) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/discussion/add-comments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({ problemId, commentInfo }),
  });

  if (!response.ok) {
    throw new Error(`Failed to post comment: ${response.status}`);
  }
}