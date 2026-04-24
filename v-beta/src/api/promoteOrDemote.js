import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * Change the role of a user account.
 *
 * @param {import("firebase/auth").User} user - The authenticated user performing the role change.
 * @param {number} accountId - The ID of the account to change the role for.
 * @param {string} roleType - The new role type (e.g., "climber", "setter", "admin").
 */

export async function changeAccountRole(user, accountId, roleType) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/api/accounts/${accountId}/role`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify({ roleType }),
    });

    if (!response.ok) {
        toast.error(`Failed to update account role: ${response.status}`);
        throw new Error(`Failed to update account role: ${response.status}`);
    }
}