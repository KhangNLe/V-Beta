import { API_BASE_URL } from "@/app/envExports";
import { getAuth, deleteUser } from "firebase/auth";
import { toast } from "react-toastify";

/**
 * @param {import("firebase/auth").User} user
 * @returns {Promise<{userId: number, username: string, email: string, role: string}>}
 */
export async function fetchAccountInfo(user) {
  let idToken = await user.getIdToken();
  let response = await fetch(`${API_BASE_URL}/api/account`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
  
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to fetch account info: ${response.status} ${response.statusText} - ${errorText}`);
  }
  const data = await response.json();
  return data;
}

/**
 * Delete the user's account in both the database and Firebase.
 * @param {import("firebase/auth").User} user
 * @returns {Promise<void>}
 */
export async function deleteAccount(user) {
  const idToken = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}/api/account/deletion`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${idToken}` },
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to delete account: ${response.status} ${response.statusText} - ${errorText}`);
  }

  const auth = getAuth();
  const currentUser = auth.currentUser;

  deleteUser(currentUser).then(() => {
    toast.success("Account deleted successfully.");
  }).catch((error) => {
    toast.error(`Failed to delete Firebase account: ${error.message}`);
  });
}