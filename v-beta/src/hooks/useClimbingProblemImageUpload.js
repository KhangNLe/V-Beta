"use client";

import { useCallback, useRef, useState } from "react";
import { toast } from "react-toastify";
import {
  isAllowedWallImageFile,
  prepareWallImageFile,
  uploadClimbingProblemImage,
  WALL_IMAGE_ACCEPT,
} from "@/api/socialImage";

/** @param {Blob} file */
function previewObjectUrl(file) {
  try {
    return URL.createObjectURL(file);
  } catch {
    return null;
  }
}

/** @param {string | null} url */
function revokeObjectUrl(url) {
  if (!url) return;
  try {
    URL.revokeObjectURL(url);
  } catch {
    /* ignore */
  }
}

/**
 * Stages a problem photo locally. The bucket upload runs only when `uploadPending` is called.
 *
 * @param {{
 *   user: import("firebase/auth").User | null | undefined,
 *   problemId: number | null | undefined,
 * }} options
 */
export function useClimbingProblemImageUpload({ user, problemId }) {
  const fileInputRef = useRef(/** @type {HTMLInputElement | null} */ (null));
  const [pendingFile, setPendingFile] = useState(/** @type {File | null} */ (null));
  const [previewURL, setPreviewURL] = useState(/** @type {string | null} */ (null));
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [convertingHeic, setConvertingHeic] = useState(false);

  const clearPending = useCallback(() => {
    setPendingFile(null);
    setPreviewURL((prev) => {
      revokeObjectUrl(prev);
      return null;
    });
  }, []);

  const openFilePicker = useCallback(() => {
    if (uploading || convertingHeic) return;
    fileInputRef.current?.click();
  }, [convertingHeic, uploading]);

  const handleFileSelected = useCallback(
    async (event) => {
      const input = event.target;
      const file = input.files?.[0] ?? null;
      input.value = "";

      if (!file || uploading || convertingHeic) return;

      if (!isAllowedWallImageFile(file)) {
        toast.error(
          "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).",
        );
        return;
      }

      try {
        const prepared = await prepareWallImageFile(file, {
          onHeicConvertStart: () => setConvertingHeic(true),
        });
        setPreviewURL((prev) => {
          revokeObjectUrl(prev);
          return previewObjectUrl(prepared);
        });
        setPendingFile(prepared);
      } catch (err) {
        console.error("Prepare problem photo failed:", err);
        toast.error(
          err instanceof Error
            ? err.message
            : "Could not read that photo. Try another image.",
        );
      } finally {
        setConvertingHeic(false);
      }
    },
    [convertingHeic, uploading],
  );

  const uploadPending = useCallback(async () => {
    if (!pendingFile || !user || problemId == null) return null;

    try {
      setUploading(true);
      setUploadProgress(0);
      return await uploadClimbingProblemImage(user, problemId, pendingFile, {
        onProgress: setUploadProgress,
      });
    } catch (err) {
      console.error("Problem image upload failed:", err);
      if (!(err instanceof Error && err.message)) {
        toast.error("Failed to upload problem photo.");
      }
      throw err;
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  }, [pendingFile, user, problemId]);

  const fileInputProps = {
    ref: fileInputRef,
    type: "file",
    accept: WALL_IMAGE_ACCEPT,
    className: "hidden",
    "aria-hidden": true,
    tabIndex: -1,
    onChange: handleFileSelected,
  };

  return {
    pendingFile,
    previewURL,
    convertingHeic,
    uploading,
    uploadProgress,
    openFilePicker,
    fileInputProps,
    uploadPending,
    clearPending,
  };
}
