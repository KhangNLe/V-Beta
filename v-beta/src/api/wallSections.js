import { API_BASE_URL } from '@/app/envExports';
import { toast } from 'react-toastify';

/**
 * @param {import("firebase/auth").User} user
 * @returns {Promise<import("@/types/wallSection").WallSection[]>}
 */
export async function fetchWallSectionsForUser(user) {
  const headers = {};

  if (user) {
    const idToken = await user.getIdToken();
    headers.Authorization = `Bearer ${idToken}`;
  }
  const response = await fetch(`${API_BASE_URL}/api/home/wall-sections`, {
    headers,
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status}`);
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

/**
 * Problems for a wall section. Maps to `GET /api/home/wall-sections/{id}/problems`.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} sectionId
 * @returns {Promise<import("@/types/climbingProblem").ClimbingProblem[]>}
 */
export async function fetchWallSectionProblemsForUser(user, sectionId) {
  const headers = {};

  if (user) {
    const idToken = await user.getIdToken();
    headers.Authorization = `Bearer ${idToken}`;
  }

  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-sections/${sectionId}/problems`,
    {
      headers,
    },
  );
  if (!response.ok) {
    throw new Error(
      `Failed to fetch wall section problems: ${response.status}`,
    );
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

/**
 * Active problems in a wall section filtered by inclusive grade range and optional sort.
 * Maps to `GET /api/search/{sectionId}?min=&max=&sort=`.
 *
 * @param {import("firebase/auth").User | null} user
 * @param {number} sectionId
 * @param {{ min: string, max: string, sort?: "asc" | "desc" }} options
 * @returns {Promise<import("@/types/climbingProblem").ClimbingProblem[]>}
 */
export async function fetchFilteredWallSectionProblems(user, sectionId, options) {
  const headers = {};

  if (user) {
    const idToken = await user.getIdToken();
    headers.Authorization = `Bearer ${idToken}`;
  }

  const params = new URLSearchParams({
    min: options.min,
    max: options.max,
  });
  if (options.sort === "asc" || options.sort === "desc") {
    params.set("sort", options.sort);
  }

  const response = await fetch(
    `${API_BASE_URL}/api/search/${sectionId}?${params.toString()}`,
    { headers },
  );
  if (!response.ok) {
    throw new Error(
      `Failed to filter wall section problems: ${response.status}`,
    );
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
  const headers = {};

  if (user) {
    const idToken = await user.getIdToken();
    headers.Authorization = `Bearer ${idToken}`;
  }

  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-sections/${sectionId}/problems/${problemId}`,
    {
      headers,
    },
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch problem: ${response.status}`);
  }

  const data = await response.json();
  const problemObject =
    data.climbingProblem ||
    Object.values(data).find(
      (value) => value && typeof value === 'object' && 'problemId' in value,
    ) ||
    {};

  return {
    ...problemObject,
    perceiveGrade:
      typeof data.perceiveGrade === 'string' ? data.perceiveGrade : '',
    discussion: Array.isArray(data.discussion) ? data.discussion : [],
  };
}

/**
 * Add Wall Section into the server (admin only)
 *
 * @param {import("firebase/auth").User} user
 * @param {{wallSectionName: string, wallSectionInfo: string}} requestPayload
 */
export async function addWallSection(user, requestPayload) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/home/wall-section/creation`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${idToken}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
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
  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-section/${wallSectionId}/delete`,
    {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${idToken}` },
    },
  );

  if (!response.ok) {
    toast.error(`Failed to delete wall section: ${response.status}`);
  }
}

/**
 * Reset a wall section from the server
 *
 * @param {import("firebase/auth").User} user
 * @param {number} wallSectionId
 */
export async function resetWallSection(user, wallSectionId) {
  const idToken = await user.getIdToken();
  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-section/${wallSectionId}/reset`,
    {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${idToken}` },
    },
  );

  if (!response.ok) {
    throw new Error(`Failed to reset wall section: ${response.status}`);
  }
}

/**
 * Create a climbing problem under a wall section (setter / admin per server rules).
 *
 * @param {import("firebase/auth").User} user
 * @param {number} sectionId
 * @param {{ holdColor: string, info: string, assignedGrade: string }} body assignedGrade must match server GradeDefinition (e.g. V4, VB)
 * @returns {Promise<Record<string, unknown>>}
 */
export async function createWallSectionProblem(user, sectionId, body) {
  const idToken = await user.getIdToken();
  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-sections/${sectionId}/problems/create`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${idToken}`,
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(body),
    },
  );

  if (!response.ok) {
    throw new Error(`Failed to create problem: ${response.status}`);
  }

  return response.json();
}

/**
 * Delete a climbing problem; returns the updated list of active problems for the section.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} sectionId
 * @param {number} problemId
 * @returns {Promise<import("@/types/climbingProblem").ClimbingProblem[]>}
 */
export async function deleteWallSectionProblem(user, sectionId, problemId) {
  const idToken = await user.getIdToken();
  const response = await fetch(
    `${API_BASE_URL}/api/home/wall-sections/${sectionId}/problems/${problemId}/delete`,
    {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${idToken}` },
    },
  );

  if (!response.ok) {
    throw new Error(`Failed to delete problem: ${response.status}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}
