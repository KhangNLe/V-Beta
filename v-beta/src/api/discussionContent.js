import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * Save a discussion post that has both a comment and an uploaded beta video.
 * Call after the video has been uploaded through the signed URL flow.
 *
 * @param {import("firebase/auth").User} user
 * @param {{problemId: number, commentInfo: string, objectFileName: string, videoURL: string}} payload
 */
export async function saveDiscussionContent(user, payload) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/api/discussion/discussion-content`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "Unknown error");
        const message = `Failed to save discussion post: ${response.status} ${errorText}`;
        toast.error(message);
        throw new Error(message);
    }

    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return null;
}

/**
 * Soft-delete a discussion post that has both a comment and a beta video.
 * Does not remove the GCS object.
 *
 * @param {import("firebase/auth").User} user
 * @param {{authorId: number, discussionId: number, problemId: number, commentContent: string, videoURL: string, deleteReason: string}} payload
 */
export async function deleteDiscussionContent(user, payload) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/api/discussion/discussion-content`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "Unknown error");
        const message = `Failed to delete discussion post: ${response.status} ${errorText}`;
        toast.error(message);
        throw new Error(message);
    }
}
