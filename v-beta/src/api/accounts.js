import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";
import { getAccountId, getAccountRole } from "@/lib/accountSession";

/**
 * @typedef {Object} Account
 * @property {number | null} id
 * @property {number | null} userId
 * @property {string} username
 * @property {string} email
 * @property {string} role
 * @property {string} roleName
 */

/**
 * @param {unknown} raw
 * @returns {Account}
 */
function normalizeListedAccount(raw) {
  const value = raw && typeof raw === "object" ? raw : {};
  const id = getAccountId(value);
  const role = getAccountRole(value);
  const record = /** @type {Record<string, unknown>} */ (value);
  return {
    id,
    userId: id,
    username: typeof record.username === "string" ? record.username : "",
    email: typeof record.email === "string" ? record.email : "",
    role,
    roleName: role,
  };
}

/**
 * Fetch all user accounts.
 * Maps to `GET /api/accounts`. Authorization failures are returned as 404.
 *
 * @param {import("firebase/auth").User} user
 * @returns {Promise<Account[]>}
 */
export async function fetchAllAccounts(user) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/accounts`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403 || response.status === 404) {
      toast.error("Access denied: You do not have permission to view accounts.");
      throw new Error("Access denied.");
    }
    toast.error(`Failed to fetch accounts: ${response.status}`);
    throw new Error(`Failed to fetch accounts: ${response.status}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data.map(normalizeListedAccount) : [];
}
