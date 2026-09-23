"use client";

import { useCallback, useRef, useState } from "react";
import { toast } from "react-toastify";
import {
  isAllowedWallImageFile,
  prepareWallImageFile,
  uploadWallSectionImage,
  WALL_IMAGE_ACCEPT,
} from "@/api/socialImage";

/**
 * Admin wall-section image upload helpers (replace/upload via signed URL).
 * Photo removal is handled by wall-section update (null image fields).
 *
 * @param {{
 *   user: import("firebase/auth").User | null | undefined,
 *   wallSectionId: number | null | undefined,
 *   onImageChange?: (imageURL: string | null) => void,
 * }} options
 */
export function useWallSectionImageUpload({ user, wallSectionId, onImageChange }) {
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

      if (!file || !user || wallSectionId == null || busy) return;

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
        const publicURL = await uploadWallSectionImage(
          user,
          wallSectionId,
          prepared,
          { onProgress: setUploadProgress },
        );
        onImageChange?.(publicURL);
        toast.success("Wall photo updated.");
      } catch (err) {
        console.error("Wall image upload failed:", err);
        if (!(err instanceof Error && err.message)) {
          toast.error("Failed to upload wall photo.");
        }
      } finally {
        setUploading(false);
        setUploadProgress(0);
      }
    },
    [busy, onImageChange, user, wallSectionId],
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
