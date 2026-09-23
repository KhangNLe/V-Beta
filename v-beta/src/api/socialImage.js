import { API_BASE_URL } from "@/app/envExports";
import { toast } from "react-toastify";

/**
 * File picker accept list for Android/iPhone camera-roll photos.
 * `image/*` is required for mobile pickers; HEIC/HEIF cover default iPhone formats.
 */
export const WALL_IMAGE_ACCEPT =
  "image/*,image/jpeg,image/jpg,image/pjpeg,image/png,image/webp,image/heic,image/heif,image/heic-sequence,image/heif-sequence,.jpg,.jpeg,.png,.webp,.heic,.heif";

const UPLOADABLE_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
]);

const HEIC_TYPES = new Set([
  "image/heic",
  "image/heif",
  "image/heic-sequence",
  "image/heif-sequence",
]);

/**
 * @param {{ name?: string, type?: string } | null | undefined} file
 */
function fileExtension(file) {
  const name = (file?.name || "").toLowerCase();
  const lastDot = name.lastIndexOf(".");
  if (lastDot < 0 || lastDot === name.length - 1) return "";
  return name.slice(lastDot + 1);
}

/**
 * @param {{ name?: string, type?: string } | null | undefined} file
 */
function isHeicLike(file) {
  const type = (file?.type || "").trim().toLowerCase();
  if (HEIC_TYPES.has(type)) return true;
  const ext = fileExtension(file);
  return ext === "heic" || ext === "heif";
}

/**
 * iOS sometimes omits the MIME type and extension. HEIC files start with an ftyp box.
 *
 * @param {File} file
 * @returns {Promise<boolean>}
 */
async function looksLikeHeic(file) {
  if (!file || isHeicLike(file) || resolveImageContentType(file)) return false;
  try {
    const header = new Uint8Array(await file.slice(0, 32).arrayBuffer());
    const ascii = String.fromCharCode(...header);
    return (
      ascii.includes("ftypheic") ||
      ascii.includes("ftypheix") ||
      ascii.includes("ftypheif") ||
      ascii.includes("ftypmif1") ||
      ascii.includes("ftypmsf1")
    );
  } catch {
    return false;
  }
}

/**
 * Normalize a browser File MIME to a type the server accepts after preparation.
 * Returns null for unsupported / unknown files (including HEIC before conversion).
 *
 * @param {File | { name?: string, type?: string } | null | undefined} file
 * @returns {string | null}
 */
export function resolveImageContentType(file) {
  const raw = (file?.type || "").trim().toLowerCase();
  if (raw === "image/jpg" || raw === "image/pjpeg") return "image/jpeg";
  if (UPLOADABLE_IMAGE_TYPES.has(raw)) return raw;

  const ext = fileExtension(file);
  if (ext === "jpg" || ext === "jpeg") return "image/jpeg";
  if (ext === "png") return "image/png";
  if (ext === "webp") return "image/webp";

  // iOS/Android camera-roll picks sometimes omit MIME and extension quirks.
  if (!raw && !ext) return null;
  return null;
}

/**
 * Whether the picker file is a phone photo we can accept (including HEIC for conversion).
 *
 * @param {File | null | undefined} file
 * @returns {boolean}
 */
export function isAllowedWallImageFile(file) {
  if (!file) return false;
  if (isHeicLike(file)) return true;
  if (resolveImageContentType(file)) return true;
  // Empty MIME from mobile pickers with a photo-like name.
  const ext = fileExtension(file);
  if (!file.type && (ext === "" || ext === "jpg" || ext === "jpeg" || ext === "png" || ext === "webp")) {
    // Nameless camera captures often arrive as image.jpg / IMG_xxxx without type.
    return Boolean(file.name);
  }
  // Some Android browsers report only image/* family via type checks already covered.
  return false;
}

/**
 * @param {CanvasImageSource} source
 * @param {number} width
 * @param {number} height
 * @returns {Promise<Blob | null>}
 */
function drawToJpegBlob(source, width, height) {
  if (!width || !height || typeof document === "undefined") return Promise.resolve(null);

  const maxEdge = 4096;
  const scale = Math.min(1, maxEdge / Math.max(width, height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, Math.round(width * scale));
  canvas.height = Math.max(1, Math.round(height * scale));
  const ctx = canvas.getContext("2d", { alpha: false });
  if (!ctx) return Promise.resolve(null);
  ctx.drawImage(source, 0, 0, canvas.width, canvas.height);

  return new Promise((resolve) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob);
          return;
        }
        try {
          const dataUrl = canvas.toDataURL("image/jpeg", 0.92);
          const [header, data] = dataUrl.split(",");
          if (!header?.includes("image/jpeg") || !data) {
            resolve(null);
            return;
          }
          const binary = atob(data);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
          resolve(new Blob([bytes], { type: "image/jpeg" }));
        } catch {
          resolve(null);
        }
      },
      "image/jpeg",
      0.92,
    );
  });
}

/**
 * Safari/iOS can decode HEIC natively. Chrome cannot, so this returns null there.
 *
 * @param {File} file
 * @returns {Promise<Blob | null>}
 */
async function decodeHeicWithBrowser(file) {
  if (typeof createImageBitmap === "function") {
    try {
      const bitmap = await createImageBitmap(file);
      try {
        const blob = await drawToJpegBlob(bitmap, bitmap.width, bitmap.height);
        if (blob) return blob;
      } finally {
        bitmap.close?.();
      }
    } catch {
      // Fall through to an Image element decode.
    }
  }

  if (typeof document === "undefined" || typeof Image === "undefined") return null;

  const url = URL.createObjectURL(file);
  try {
    const image = await new Promise((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error("browser could not decode image"));
      el.src = url;
    });
    return drawToJpegBlob(image, image.naturalWidth, image.naturalHeight);
  } catch {
    return null;
  } finally {
    URL.revokeObjectURL(url);
  }
}

/**
 * @param {File} file
 * @returns {Promise<Blob>}
 */
async function decodeHeicWithLibrary(file) {
  const mod = await import("heic-to/next");
  const heicTo = mod.heicTo;
  if (typeof heicTo !== "function") {
    throw new Error("HEIC converter failed to load.");
  }
  const converted = await heicTo({
    blob: file,
    type: "image/jpeg",
    quality: 0.92,
  });
  if (!converted) throw new Error("HEIC converter returned no image.");
  return converted;
}

/**
 * @param {File} sourceFile
 * @param {Blob} jpegBlob
 */
function jpegFileFromBlob(sourceFile, jpegBlob) {
  const baseName = (sourceFile.name || "photo").replace(/\.(heic|heif)$/i, "");
  return new File([jpegBlob], `${baseName || "photo"}.jpg`, {
    type: "image/jpeg",
    lastModified: Date.now(),
  });
}

/**
 * Convert iPhone HEIC/HEIF (and odd mobile MIME cases) into a JPEG/PNG/WebP File
 * the signed-upload API and cross-browser img tags can use.
 *
 * @param {File} file
 * @param {{ onHeicConvertStart?: () => void }} [options]
 * @returns {Promise<File>}
 */
export async function prepareWallImageFile(file, options = {}) {
  if (!file) {
    throw new Error("No image file selected.");
  }

  if (isHeicLike(file) || (await looksLikeHeic(file))) {
    options.onHeicConvertStart?.();
    const nativeJpeg = await decodeHeicWithBrowser(file);
    if (nativeJpeg) return jpegFileFromBlob(file, nativeJpeg);

    try {
      const jpegBlob = await decodeHeicWithLibrary(file);
      return jpegFileFromBlob(file, jpegBlob);
    } catch (err) {
      console.error("HEIC conversion failed:", err);
      throw new Error(
        "Could not convert this iPhone photo. Try exporting it as JPEG, or disable HEIF in Camera settings (Most Compatible).",
      );
    }
  }

  let contentType = resolveImageContentType(file);
  // Mobile cameras often leave type empty for JPEG captures.
  if (!contentType && !file.type) {
    const ext = fileExtension(file);
    if (!ext || ext === "jpg" || ext === "jpeg") {
      contentType = "image/jpeg";
      const safeName = file.name?.includes(".")
        ? file.name
        : `${file.name || "photo"}.jpg`;
      return new File([file], safeName, {
        type: "image/jpeg",
        lastModified: file.lastModified || Date.now(),
      });
    }
  }

  if (!contentType) {
    throw new Error(
      "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).",
    );
  }

  if (file.type && file.type.toLowerCase() !== contentType) {
    return new File([file], file.name || `photo.${contentType.split("/")[1]}`, {
      type: contentType,
      lastModified: file.lastModified || Date.now(),
    });
  }

  return file;
}


/**
 * Request a signed GCS PUT URL for an image upload.
 *
 * @param {import("firebase/auth").User} user
 * @param {{
 *   fileName: string,
 *   contentType: string,
 *   imageTargetType: "WALL_SECTION" | "CLIMBING_PROBLEM" | "USER_ACCOUNT",
 *   wallSectionId?: number,
 *   problemId?: number,
 *   userid?: number,
 * }} payload
 * @returns {Promise<{
 *   signedURL: string,
 *   method?: string,
 *   uploadObjectName?: string,
 *   publicURL?: string,
 * }>}
 */
export async function requestImageSignedUrl(user, payload) {
  const idToken = await user.getIdToken();
  const params = new URLSearchParams({
    fileName: payload.fileName,
    contentType: payload.contentType,
    imageTargetType: payload.imageTargetType,
  });
  if (payload.wallSectionId != null) {
    params.set("wallSectionId", String(payload.wallSectionId));
  }
  if (payload.problemId != null) {
    params.set("problemId", String(payload.problemId));
  }
  if (payload.userid != null) {
    params.set("userid", String(payload.userid));
  }

  const response = await fetch(
    `${API_BASE_URL}/api/social/image/signed-url?${params.toString()}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    },
  );

  if (!response.ok) {
    const errorText = await response.text().catch(() => "");
    const message =
      errorText.trim() ||
      `Failed to request image upload URL: ${response.status}`;
    toast.error(message);
    throw new Error(message);
  }

  return response.json();
}

/**
 * Upload image bytes to GCS via signed URL, optionally reporting progress.
 *
 * @param {File} file
 * @param {{ signedURL: string, method?: string, contentType?: string }} signedResponse
 * @param {{ onProgress?: (percent: number) => void }} [options]
 * @returns {Promise<{ ok: true, status: number }>}
 */
export function uploadImageToSignedUrl(file, signedResponse, options = {}) {
  const { onProgress } = options;
  const contentType =
    signedResponse.contentType ||
    resolveImageContentType(file) ||
    "application/octet-stream";

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(signedResponse.method || "PUT", signedResponse.signedURL);

    xhr.upload.onprogress = (event) => {
      if (!onProgress || !event.lengthComputable || event.total <= 0) return;
      onProgress(Math.min(100, Math.round((event.loaded / event.total) * 100)));
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.(100);
        resolve({ ok: true, status: xhr.status });
        return;
      }
      const message = `Failed to upload image: ${xhr.status} ${xhr.responseText || ""}`.trim();
      toast.error(message);
      reject(new Error(message));
    };

    xhr.onerror = () => {
      const message = "Failed to upload image: network error";
      toast.error(message);
      reject(new Error(message));
    };

    xhr.setRequestHeader("Content-Type", contentType);
    xhr.send(file);
  });
}

/**
 * Persist image metadata after a successful GCS PUT.
 * Bound as query params on PATCH /api/social/image/upload.
 *
 * @param {import("firebase/auth").User} user
 * @param {{
 *   targetType: "WALL_SECTION" | "CLIMBING_PROBLEM" | "USER_ACCOUNT",
 *   objectFileName: string,
 *   imageUrl: string,
 *   wallSectionId?: number,
 *   climbingProblemId?: number,
 *   userId?: number,
 * }} payload
 */
export async function saveImageMetadata(user, payload) {
  const idToken = await user.getIdToken();
  const params = new URLSearchParams({
    targetType: payload.targetType,
    objectFileName: payload.objectFileName,
    imageUrl: payload.imageUrl,
  });
  if (payload.wallSectionId != null) {
    params.set("wallSectionId", String(payload.wallSectionId));
  }
  if (payload.climbingProblemId != null) {
    params.set("climbingProblemId", String(payload.climbingProblemId));
  }
  if (payload.userId != null) {
    params.set("userId", String(payload.userId));
  }

  const response = await fetch(
    `${API_BASE_URL}/api/social/image/upload?${params.toString()}`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    },
  );

  if (!response.ok) {
    const errorText = await response.text().catch(() => "");
    const message =
      errorText.trim() ||
      `Failed to save image metadata: ${response.status}`;
    toast.error(message);
    throw new Error(message);
  }
}

/**
 * Delete a wall section image from storage and clear metadata.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} wallSectionId
 */
export async function removeWallSectionImage(user, wallSectionId) {
  const idToken = await user.getIdToken();
  const params = new URLSearchParams({
    wallSectionId: String(wallSectionId),
  });

  const response = await fetch(
    `${API_BASE_URL}/api/social/image/wall?${params.toString()}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    },
  );

  if (!response.ok) {
    const errorText = await response.text().catch(() => "");
    const message =
      errorText.trim() ||
      `Failed to remove wall image: ${response.status}`;
    toast.error(message);
    throw new Error(message);
  }
}

/**
 * Full wall-section image upload: signed URL → GCS PUT → save metadata.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} wallSectionId
 * @param {File} file
 * @param {{ onProgress?: (percent: number) => void }} [options]
 * @returns {Promise<string>} public image URL
 */
export async function uploadWallSectionImage(user, wallSectionId, file, options = {}) {
  let uploadFile;
  try {
    uploadFile = await prepareWallImageFile(file);
  } catch (err) {
    const message =
      err instanceof Error
        ? err.message
        : "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).";
    toast.error(message);
    throw new Error(message);
  }

  const contentType = resolveImageContentType(uploadFile);
  if (!contentType) {
    const message =
      "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).";
    toast.error(message);
    throw new Error(message);
  }

  const signedData = await requestImageSignedUrl(user, {
    fileName: uploadFile.name,
    contentType,
    imageTargetType: "WALL_SECTION",
    wallSectionId,
  });

  if (!signedData?.signedURL) {
    const message = "Signed URL response is missing signedURL.";
    toast.error(message);
    throw new Error(message);
  }

  const uploadObjectName =
    signedData.uploadObjectName || signedData.objectName || "";
  if (!uploadObjectName) {
    const message = "Signed URL response is missing uploadObjectName.";
    toast.error(message);
    throw new Error(message);
  }

  const publicURL = signedData.publicURL || "";
  if (!publicURL) {
    const message = "Signed URL response is missing publicURL.";
    toast.error(message);
    throw new Error(message);
  }

  await uploadImageToSignedUrl(
    uploadFile,
    { ...signedData, contentType },
    { onProgress: options.onProgress },
  );

  await saveImageMetadata(user, {
    targetType: "WALL_SECTION",
    objectFileName: uploadObjectName,
    imageUrl: publicURL,
    wallSectionId,
  });

  return publicURL;
}

/**
 * Full climbing-problem image upload: signed URL → GCS PUT → save metadata.
 *
 * @param {import("firebase/auth").User} user
 * @param {number} problemId
 * @param {File} file
 * @param {{ onProgress?: (percent: number) => void }} [options]
 * @returns {Promise<string>} public image URL
 */
export async function uploadClimbingProblemImage(user, problemId, file, options = {}) {
  let uploadFile;
  try {
    uploadFile = await prepareWallImageFile(file);
  } catch (err) {
    const message =
      err instanceof Error
        ? err.message
        : "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).";
    toast.error(message);
    throw new Error(message);
  }

  const contentType = resolveImageContentType(uploadFile);
  if (!contentType) {
    const message =
      "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).";
    toast.error(message);
    throw new Error(message);
  }

  const signedData = await requestImageSignedUrl(user, {
    fileName: uploadFile.name,
    contentType,
    imageTargetType: "CLIMBING_PROBLEM",
    problemId,
  });

  if (!signedData?.signedURL) {
    const message = "Signed URL response is missing signedURL.";
    toast.error(message);
    throw new Error(message);
  }

  const uploadObjectName =
    signedData.uploadObjectName || signedData.objectName || "";
  if (!uploadObjectName) {
    const message = "Signed URL response is missing uploadObjectName.";
    toast.error(message);
    throw new Error(message);
  }

  const publicURL = signedData.publicURL || "";
  if (!publicURL) {
    const message = "Signed URL response is missing publicURL.";
    toast.error(message);
    throw new Error(message);
  }

  await uploadImageToSignedUrl(
    uploadFile,
    { ...signedData, contentType },
    { onProgress: options.onProgress },
  );

  await saveImageMetadata(user, {
    targetType: "CLIMBING_PROBLEM",
    objectFileName: uploadObjectName,
    imageUrl: publicURL,
    climbingProblemId: problemId,
  });

  return publicURL;
}
