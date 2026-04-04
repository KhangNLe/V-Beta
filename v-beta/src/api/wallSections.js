import { API_BASE_URL } from "@/app/envExports";

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
 * @returns {Promise<import("@/types/climbProblem").ClimbProblem>}
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
