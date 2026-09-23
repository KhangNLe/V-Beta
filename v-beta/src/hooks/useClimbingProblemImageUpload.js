"use client";

import { useCallback, useRef, useState } from "react";
import { toast } from "react-toastify";
import {
  isAllowedWallImageFile,
  prepareWallImageFile,
  uploadClimbingProblemImage,
  WALL_IMAGE_ACCEPT,
} from "@/api/socialImage";

/**
 * Setter climbing-problem image upload helpers (replace/upload via signed URL).
 * Photo removal is handled by problem update (null image fields).
 *
 * @param {{
 *   user: import("firebase/auth").User | null | undefined,
 *   problemId: number | null | undefined,
 *   onImageChange?: (imageURL: string | null) => void,
 * }} options
 */
export function useClimbingProblemImageUpload({ user, problemId, onImageChange }) {
  const fileInputRef = useRef(/** @type {HTMLInputElement | null} */ (null));
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const busy = uploading;

  const openFilePicker = useCallback(() => {
    if (busy) return;
    fileInputRef.current?.click();
  }, [busy]);

  const handleFileSelected = useCallback(
    async (event) => {
      const input = event.target;
      const file = input.files?.[0] ?? null;
      input.value = "";

      if (!file || !user || problemId == null || busy) return;

      if (!isAllowedWallImageFile(file)) {
        toast.error(
          "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).",
        );
        return;
      }

      try {
        setUploading(true);
        setUploadProgress(0);
        const prepared = await prepareWallImageFile(file);
        const publicURL = await uploadClimbingProblemImage(
          user,
          problemId,
          prepared,
          { onProgress: setUploadProgress },
        );
        onImageChange?.(publicURL);
        toast.success("Problem photo updated.");
      } catch (err) {
        console.error("Problem image upload failed:", err);
        if (!(err instanceof Error && err.message)) {
          toast.error("Failed to upload problem photo.");
        }
      } finally {
        setUploading(false);
        setUploadProgress(0);
      }
    },
    [busy, onImageChange, user, problemId],
  );

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
    busy,
    uploading,
    uploadProgress,
    openFilePicker,
    fileInputProps,
  };
}
