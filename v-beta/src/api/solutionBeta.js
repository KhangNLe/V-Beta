import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * Request signed upload URL for a solution beta video.
 *
 * @param {import("firebase/auth").User} user
 * @param {{fileName: string, contentType: string, problemId: number, wallSectionId: number}} payload
 */
export async function requestSignedUploadUrl(user, { fileName, contentType, problemId, wallSectionId }) {
    const idToken = await user.getIdToken();

    const response = await fetch(`${API_BASE_URL}/api/discussion/solution-beta/upload-url`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify({
            fileName,
            contentType,
            problemId,
            wallSectionId,
        }),
    });

    if (!response.ok) {
        const message = `Failed to request signed upload URL: ${response.status}`;
        toast.error(message);
        throw new Error(message);
    }

    return response.json();
}

/**
 * Upload the solution beta file to GCS using signed URL response.
 *
 * @param {File} file
 * @param {{signedURL: string, method?: string}} signedResponse
 */
export async function uploadSolutionBeta(file, signedResponse) {
    let request;
    try {
        request = await fetch(signedResponse.signedURL, {
            method: signedResponse.method || "PUT",
            headers: {
                "Content-Type": file.type || "application/octet-stream",
            },
            body: file,
        });
    } catch (error) {
        const message = error instanceof Error ? error.message : "Unknown upload error";
        const toastMessage = `Failed to upload solution beta: ${message}`;
        toast.error(toastMessage);
        throw new Error(toastMessage);
    }

    if (!request.ok) {
        const errorText = await request.text().catch(() => "Unknown error");
        const message = `Failed to upload solution beta: ${request.status} ${errorText}`;
        toast.error(message);
        throw new Error(message);
    }

    return {
        ok: true,
        status: request.status,
    };
}

/**
 * Save solution beta metadata to database.
 *
 * @param {import("firebase/auth").User} user
 * @param {{problemId: number, objectFileName: string, videoURL: string}} payload
 */
export async function saveSolutionBetaToDatabase(user, payload) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/api/discussion/solution-beta/save`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "Unknown error");
        const message = `Failed to save solution beta metadata: ${response.status} ${errorText}`;
        toast.error(message);
        throw new Error(message);
    }

    // Supports both empty and JSON success responses.
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return null;
}

/**
 * Delete solution beta from database.
 *
 * @param {import("firebase/auth").User} user
 * @param {{userId: number, problemId: number, discussionId: number, publicUrl: string}} payload
 */
export async function deleteSolutionBetaFromDatabase(user, payload) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/api/discussion/solution-beta`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "Unknown error");
        const message = `Failed to delete solution beta: ${response.status} ${errorText}`;
        toast.error(message);
        throw new Error(message);
    }
    return;
}