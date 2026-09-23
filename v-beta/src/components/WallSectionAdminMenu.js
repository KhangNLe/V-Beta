"use client";

import { updateWallSection } from "@/api/wallSections";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useWallSectionImageUpload } from "@/hooks/useWallSectionImageUpload";
import { buttons } from "@/ui/appTheme";
import { MoreVertical } from "lucide-react";
import { useState } from "react";
import { toast } from "react-toastify";

const WALL_SECTION_PLACEHOLDER_SRC = "/co-op.png";

/** @param {{ imageURL?: string | null, imageUrl?: string | null } | null | undefined} section */
function sectionImageURL(section) {
  return section?.imageURL ?? section?.imageUrl ?? null;
}

/**
 * Admin menu + Edit wall dialog (name, description, upload/replace/remove photo).
 *
 * @param {{
 *   user: import("firebase/auth").User,
 *   section: {
 *     wallSectionID: number,
 *     wallSectionName?: string,
 *     wallSectionInfo?: string,
 *     imageURL?: string | null,
 *     imageUrl?: string | null,
 *   },
 *   onSectionUpdated: (patch: {
 *     wallSectionName?: string,
 *     wallSectionInfo?: string,
 *     imageURL?: string | null,
 *   }) => void,
 *   onDeleteSection?: () => void,
 *   ariaLabel?: string,
 * }} props
 */
export default function WallSectionAdminMenu({
  user,
  section,
  onSectionUpdated,
  onDeleteSection,
  ariaLabel = "Section actions",
}) {
  const wallSectionId = section.wallSectionID;
  const currentImageURL = sectionImageURL(section);

  const [editOpen, setEditOpen] = useState(false);
  const [editName, setEditName] = useState("");
  const [editInfo, setEditInfo] = useState("");
  const [saveSubmitting, setSaveSubmitting] = useState(false);
  const [removing, setRemoving] = useState(false);

  const {
    pendingFile,
    previewURL,
    convertingHeic,
    uploading,
    uploadProgress,
    openFilePicker,
    fileInputProps,
    uploadPending,
    clearPending,
  } = useWallSectionImageUpload({
    user,
    wallSectionId,
  });

  const openEditDialog = () => {
    clearPending();
    setEditName(section.wallSectionName || "");
    setEditInfo(section.wallSectionInfo || "");
    setEditOpen(true);
  };

  const dialogBusy = saveSubmitting || removing || uploading || convertingHeic;
  const hasSavedImage = Boolean(currentImageURL);
  const displaySrc = previewURL || currentImageURL || WALL_SECTION_PLACEHOLDER_SRC;

  const handleSave = async (event) => {
    event.preventDefault();
    if (!user || dialogBusy) return;

    const name = editName.trim();
    const info = editInfo.trim();
    if (!name || !info) {
      toast.error("Please enter both a name and description.");
      return;
    }

    try {
      setSaveSubmitting(true);
      let nextImageURL = currentImageURL;
      if (pendingFile) {
        nextImageURL = await uploadPending();
      }
      const updated = await updateWallSection(user, wallSectionId, {
        wallSectionName: name,
        wallSectionInfo: info,
        objectFileName: null,
        imageURL: nextImageURL,
      });
      onSectionUpdated({
        wallSectionName: updated?.wallSectionName ?? name,
        wallSectionInfo: updated?.wallSectionInfo ?? info,
        imageURL: sectionImageURL(updated) ?? nextImageURL,
      });
      clearPending();
      setEditOpen(false);
      toast.success("Wall section updated.");
    } catch (err) {
      console.error("Update wall section failed:", err);
    } finally {
      setSaveSubmitting(false);
    }
  };

  const handleRemovePhoto = async () => {
    if (!user || dialogBusy) return;

    const name = (editName || section.wallSectionName || "").trim();
    const info = (editInfo || section.wallSectionInfo || "").trim();
    if (!name || !info) {
      toast.error("Name and description are required to update this wall.");
      return;
    }

    try {
      setRemoving(true);
      clearPending();
      const updated = await updateWallSection(user, wallSectionId, {
        wallSectionName: name,
        wallSectionInfo: info,
        objectFileName: null,
        imageURL: null,
      });
      onSectionUpdated({
        wallSectionName: updated.wallSectionName ?? name,
        wallSectionInfo: updated.wallSectionInfo ?? info,
        imageURL: null,
      });
      toast.success("Wall photo removed.");
    } catch (err) {
      console.error("Remove wall photo failed:", err);
    } finally {
      setRemoving(false);
    }
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              className="shrink-0 text-muted-foreground"
              aria-label={ariaLabel}
              disabled={dialogBusy}
            />
          }
        >
          <MoreVertical className="size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem
            disabled={dialogBusy}
            onClick={openEditDialog}
          >
            Edit wall
          </DropdownMenuItem>
          {onDeleteSection && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                variant="destructive"
                disabled={dialogBusy}
                onClick={onDeleteSection}
              >
                Delete
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog
        open={editOpen}
        onOpenChange={(open) => {
          if (dialogBusy) return;
          if (!open) clearPending();
          setEditOpen(open);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Edit wall</DialogTitle>
            <DialogDescription>
              Update this wall section&apos;s details and photo.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSave} className="grid gap-4">
            <div className="grid gap-1.5">
              <label
                htmlFor={`edit-ws-name-${wallSectionId}`}
                className="text-sm font-medium text-foreground"
              >
                Name
              </label>
              <input
                id={`edit-ws-name-${wallSectionId}`}
                name="name"
                type="text"
                autoComplete="off"
                required
                disabled={dialogBusy}
                value={editName}
                onChange={(ev) => setEditName(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
              />
            </div>

            <div className="grid gap-1.5">
              <label
                htmlFor={`edit-ws-info-${wallSectionId}`}
                className="text-sm font-medium text-foreground"
              >
                Description
              </label>
              <textarea
                id={`edit-ws-info-${wallSectionId}`}
                name="info"
                rows={3}
                required
                disabled={dialogBusy}
                value={editInfo}
                onChange={(ev) => setEditInfo(ev.target.value)}
                className="resize-y rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
              />
            </div>

            <div className="grid gap-2">
              <span className="text-sm font-medium text-foreground">Photo</span>
              <div className="overflow-hidden rounded-md border border-border">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={displaySrc}
                  alt={
                    previewURL
                      ? "Selected wall section photo"
                      : hasSavedImage
                        ? ""
                        : "Default wall section photo"
                  }
                  className="aspect-[16/10] w-full object-cover"
                />
              </div>
              {!hasSavedImage && !previewURL && (
                <p className="m-0 text-sm text-muted-foreground">
                  Using the default photo until you upload one.
                </p>
              )}
              {convertingHeic && (
                <p className="m-0 text-sm text-muted-foreground" role="status">
                  Uploading iPhone photo…
                </p>
              )}
              {previewURL && (
                <p className="m-0 text-sm text-muted-foreground">
                  This photo uploads when you save changes.
                </p>
              )}

              <input {...fileInputProps} />

              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={dialogBusy}
                  onClick={openFilePicker}
                >
                  {convertingHeic
                    ? "Uploading…"
                    : pendingFile || hasSavedImage
                      ? "Replace photo"
                      : "Upload photo"}
                </Button>
                {hasSavedImage && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={dialogBusy}
                    onClick={() => {
                      void handleRemovePhoto();
                    }}
                  >
                    {removing ? "Removing…" : "Remove photo"}
                  </Button>
                )}
              </div>
            </div>

            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={dialogBusy}
                onClick={() => setEditOpen(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={dialogBusy}
                style={buttons.primary}
              >
                {saveSubmitting
                  ? pendingFile && uploadProgress > 0
                    ? `Uploading… ${uploadProgress}%`
                    : "Saving…"
                  : "Save changes"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
