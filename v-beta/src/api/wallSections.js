import { API_BASE_URL } from "@/app/envExports";

/**
 * @param {import("firebase/auth").User} user
 * @returns {Promise<import("@/types/wallSection").WallSection[]>}
 */
export async function fetchWallSectionsForUser(user) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/wall-sections`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
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
  return [];
}
