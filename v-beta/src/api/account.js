import { API_BASE_URL } from "@/app/envExports";

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