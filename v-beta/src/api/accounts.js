import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * @typedef {Object} Account
 * @property {number} id
 * @property {string} username
 * @property {string} email
 * @property {string} firebaseUid
 * @property {string} roleName
 */

/**
 * Fetch all user accounts.
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
    if (response.status === 403) {
      toast.error("Access denied: You do not have permission to view accounts.");
      throw new Error("Access denied.");
    }
    toast.error(`Failed to fetch accounts: ${response.status}`);
    throw new Error(`Failed to fetch accounts: ${response.status}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}