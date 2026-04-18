import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * @param {import("firebase/auth").User} user
 * @returns {Promise<import("@/types/wallSection").WallSection[]>}
 */
export async function fetchWallSectionsForUser(user) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-sections`, {
      headers: { Authorization: `Bearer ${idToken}` },
    }
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status}`);
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

/**
 * Problems for a wall section. Wire to `GET /api/wall-sections/{id}/problems` when available.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} sectionId
 * @returns {Promise<import("@/types/climbProblem").ClimbProblem[]>}
 */
export async function fetchWallSectionProblemsForUser(_user, _sectionId) {
  const idToken = await _user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-sections/${_sectionId}/problems`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch wall section problems: ${response.status}`);
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

/**
 * Fetch a single problem by ID within a wall section.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} sectionId
 * @param {number} problemId
 * @returns {Promise<import("@/types/climbingProblem").ClimbProblem>}
 */
export async function fetchProblemForUser(user, sectionId, problemId) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-sections/${sectionId}/problems/${problemId}`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch problem: ${response.status}`);
  }

  const data = await response.json();
  const problemObject = data.climbingProblem || Object.values(data).find((value) => value && typeof value === "object" && "problemId" in value) || {};

  return {
    ...problemObject,
    perceiveGrade: typeof data.perceiveGrade === "string" ? data.perceiveGrade : "",
    discussion: Array.isArray(data.discussion) ? data.discussion : [],
  };
}

/**
 * Add Wall Section into the server
 * 
 * @param {import("firebase/auth").User} user
 * @param {{wallSectionName: string, wallSectionInfo: string}} requestPayload
 */
export async function addWallSection(user, requestPayload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-section/creation`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${idToken}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(requestPayload),
  });

  if (!response.ok) {
    toast.error(`Failed to add wall section: ${response.status}`);
  }

  return response.json();
}

/**
 * Delete a wall section from the server
 * 
 * @param {import("firebase/auth").User} user
 * @param {number} wallSectionId
 */
export async function deleteWallSection(user, wallSectionId) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-section/${wallSectionId}/delete`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${idToken}` },
  });

  if (!response.ok) {
    toast.error(`Failed to delete wall section: ${response.status}`);
  }

  return; // No response body
}

/**
 * Reset a wall section from the server
 * 
 * @param {import("firebase/auth").User} user
 * @param {number} wallSectionId
 */
export async function resetWallSection(user, wallSectionId) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/home/wall-section/${wallSectionId}/reset`, {
    method: "GET",
    headers: { Authorization: `Bearer ${idToken}` },
  });

  if (!response.ok) {
    toast.error(`Failed to reset wall section: ${response.status}`);
  }

  return;
}