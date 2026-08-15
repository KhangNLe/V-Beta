import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * Post a new comment for a problem.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} problemId
 * @param {string} commentInfo
 */
export async function postCommentForUser(user, problemId, commentInfo) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/discussion/add-comments`, {
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

/**
 * Delete user comment from a problem.
 * 
 * @param {import("firebase/auth").User} user
 * @param {{authorId: number, problemId: number, discussionId: number, commentContent: string}} payload
 */
export async function deleteUserComment(user, payload){
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/discussion/comment/delete`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = `Failed to delete comment: ${response.status}`;
    toast.error(message);
    throw new Error(message);
  }
  return;
}

/**
 * Add user suggested grade for a problem.
 * 
 * @param {import("firebase/auth").User} user
 * @param {{perceivedGrade: string}} payload
 * @param {number} problemId
 */
export async function addUserSuggestedGrade(user, payload, problemId){
  const idToken = await user.getIdToken();

  if (idToken == null) {
    toast.error("Unauthorized action: User must log in to suggest a grade.");
    return;
  }
  
  const response = await fetch(`${API_BASE_URL}/api/discussion/problems/${problemId}/suggest-grade`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      perceiveGrade: payload.perceivedGrade,
    }),
  });

  if (!response.ok) {
    const message = `Failed to add suggested grade: ${response.status}`;
    toast.error(message);
    throw new Error(message);
  }
  return response.body;
}