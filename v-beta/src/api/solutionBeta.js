import { API_BASE_URL } from "@/app/envExports";

/**
 * Request signed upload URL for a solution beta video.
 *
 * @param {import("firebase/auth").User} user
 * @param {{fileName: string, contentType: string, problemId: number, wallSectionId: number}} payload
 */
export async function requestSignedUploadUrl(user, { fileName, contentType, problemId, wallSectionId }) {
    const idToken = await user.getIdToken();

    const response = await fetch(`${API_BASE_URL}/discussion/solution-beta/upload-url`, {
        method: "POST",
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
        throw new Error(`Failed to request signed upload URL: ${response.status}`);
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
        throw new Error(
            "Network error while uploading to signed URL. This is commonly a bucket CORS/preflight issue for browser PUT requests."
        );
    }

    if (!request.ok) {
        const errorText = await request.text().catch(() => "Unknown error");
        throw new Error(`Failed to upload solution beta: ${request.status} ${errorText}`);
    }

    return {
        ok: true,
        status: request.status,
    };
}

/**
 * TODO: Persist uploaded solution beta metadata in backend DB.
 * Expected backend route (to be implemented): POST /discussion/solution-beta
 *
 * @param {import("firebase/auth").User} user
 * @param {{problemId: number, betaName: string, videoURL: string}} payload
 */
export async function saveSolutionBetaToDatabase(user, payload) {
    const idToken = await user.getIdToken();
    const response = await fetch(`${API_BASE_URL}/discussion/solution-beta`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${idToken}`,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "Unknown error");
        throw new Error(`Failed to save solution beta metadata: ${response.status} ${errorText}`);
    }

    // Supports both empty and JSON success responses.
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return null;
}