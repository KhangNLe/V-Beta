/** Allowed solution-beta types for the file picker and signed uploads. */
export const SOLUTION_VIDEO_ACCEPT =
  "video/mp4,video/webm,.mp4,.webm,.mov,.MOV";

const MOV_TYPE = new Set(["video/quicktime", "video/x-quicktime"]);

/**
 * @param {{ name?: string, type?: string } | null | undefined} file
 * @returns {"mp4" | "webm" | "mov"}
 */
export function videoExtensionFromFile(file) {
  const name = (file?.name || "").toLowerCase();
  const type = (file?.type || "").toLowerCase();
  if (name.endsWith(".webm") || type === "video/webm") return "webm";
  if (name.endsWith(".mov") || MOV_TYPE.has(type)) return "mov";
  return "mp4";
}

/**
 * MIME type that must match the GCS signed PUT Content-Type.
 *
 * @param {{ name?: string, type?: string } | null | undefined} file
 */
export function videoUploadContentType(file) {
  const ext = videoExtensionFromFile(file);
  if (ext === "webm") return "video/webm";
  if (ext === "mov") return "video/quicktime";
  return "video/mp4";
}

/**
 * @param {string} originalFileName
 * @param {number|string} problemId
 */
export function buildShortUploadFileName(originalFileName, problemId) {
  const ext = videoExtensionFromFile({ name: originalFileName, type: "" });
  return `beta_${problemId}.${ext}`;
}

/**
 * @param {string} [objectName]
 * @param {string} [publicURL]
 */
export function toPlaybackStorageRefs(objectName = "", publicURL = "") {
  if (!/\.mov$/i.test(objectName) && !/\.mov(\?|$)/i.test(publicURL)) {
    return { objectName, publicURL };
  }
  return {
    objectName: objectName.replace(/\.mov$/i, ".mp4"),
    publicURL: publicURL.replace(/\.mov(?=\?|$)/i, ".mp4").replace(/\.mov$/i, ".mp4"),
  };
}

/**
 * Wait until the converted MP4 is publicly readable (Cloud Run transcode worker).
 *
 * @param {string} publicURL
 * @param {{ timeoutMs?: number, intervalMs?: number, fetchImpl?: typeof fetch }} [options]
 */
export async function waitForPublicVideo(publicURL, options = {}) {
  const timeoutMs = options.timeoutMs ?? 8 * 60 * 1000;
  const intervalMs = options.intervalMs ?? 3000;
  const fetchImpl = options.fetchImpl ?? fetch;
  const started = Date.now();
  let lastError = "MP4 is not available yet.";

  while (Date.now() - started <= timeoutMs) {
    try {
      const response = await fetchImpl(publicURL, { method: "HEAD" });
      if (response.ok) return;
      lastError = `HEAD ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : "network error";
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new Error(
    `iPhone video was uploaded, but conversion to MP4 timed out (${lastError}). Try again in a minute.`,
  );
}
