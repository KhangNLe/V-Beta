"use client";

import { updateClimbingProblem } from "@/api/wallSections";
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
import { useClimbingProblemImageUpload } from "@/hooks/useClimbingProblemImageUpload";
import { buttons } from "@/ui/appTheme";
import { MoreVertical } from "lucide-react";
import { useState } from "react";
import { toast } from "react-toastify";

const PROBLEM_PLACEHOLDER_SRC = "/problem-holder.jpg";

const GRADE_OPTIONS = [
  "VB",
  "V0",
  "V1",
  "V2",
  "V3",
  "V4",
  "V5",
  "V6",
  "V7",
  "V8",
  "V9",
  "V10",
  "V11",
  "V12",
  "V13",
  "V14",
  "V15",
  "V16",
  "V17",
];

/** @param {{ imageURL?: string | null, imageUrl?: string | null } | null | undefined} problem */
function problemImageURL(problem) {
  return problem?.imageURL ?? problem?.imageUrl ?? null;
}

/** @param {string | null | undefined} raw */
function normalizeGrade(raw) {
  const value = String(raw || "").trim().toUpperCase();
  return GRADE_OPTIONS.includes(value) ? value : "VB";
}

/**
 * Setter menu + Edit problem dialog (hold color, grade, notes, upload/replace/remove photo).
 *
 * @param {{
 *   user: import("firebase/auth").User,
 *   wallSectionId: number,
 *   problem: {
 *     problemId: number,
 *     holdColor?: string,
 *     info?: string,
 *     assignedGrade?: string,
 *     imageURL?: string | null,
 *     imageUrl?: string | null,
 *   },
 *   onProblemUpdated: (patch: {
 *     holdColor?: string,
 *     info?: string,
 *     assignedGrade?: string,
 *     imageURL?: string | null,
 *   }) => void,
 *   onDeleteProblem?: () => void,
 *   ariaLabel?: string,
 * }} props
 */
export default function ClimbingProblemSetterMenu({
  user,
  wallSectionId,
  problem,
  onProblemUpdated,
  onDeleteProblem,
  ariaLabel = "Problem actions",
}) {
  const problemId = problem.problemId;
  const currentImageURL = problemImageURL(problem);

  const [editOpen, setEditOpen] = useState(false);
  const [editHoldColor, setEditHoldColor] = useState("");
  const [editGrade, setEditGrade] = useState("VB");
  const [editInfo, setEditInfo] = useState("");
  const [saveSubmitting, setSaveSubmitting] = useState(false);
  const [removing, setRemoving] = useState(false);

  const {
    busy: imageBusy,
    uploading,
    uploadProgress,
    openFilePicker,
    fileInputProps,
  } = useClimbingProblemImageUpload({
    user,
    problemId,
    onImageChange: (nextImageURL) => {
      onProblemUpdated({ imageURL: nextImageURL });
    },
  });

  const openEditDialog = () => {
    setEditHoldColor(problem.holdColor || "");
    setEditGrade(normalizeGrade(problem.assignedGrade));
    setEditInfo(problem.info || "");
    setEditOpen(true);
  };

  const dialogBusy = saveSubmitting || removing || imageBusy;
  const hasImage = Boolean(currentImageURL);

  const handleSave = async (event) => {
    event.preventDefault();
    if (!user || dialogBusy) return;

    const holdColor = editHoldColor.trim();
    const info = editInfo.trim();
    if (!holdColor || !info) {
      toast.error("Please enter both a hold color and notes.");
      return;
    }

    try {
      setSaveSubmitting(true);
      const updated = await updateClimbingProblem(user, wallSectionId, problemId, {
        holdColor,
        info,
        assignedGrade: editGrade,
        objectFileName: null,
        imageURL: currentImageURL,
      });
      onProblemUpdated({
        holdColor: updated.holdColor ?? holdColor,
        info: updated.info ?? info,
        assignedGrade: updated.assignedGrade ?? editGrade,
        imageURL: problemImageURL(updated) ?? currentImageURL,
      });
      setEditOpen(false);
      toast.success("Problem updated.");
    } catch (err) {
      console.error("Update problem failed:", err);
    } finally {
      setSaveSubmitting(false);
    }
  };

  const handleRemovePhoto = async () => {
    if (!user || dialogBusy) return;

    const holdColor = (editHoldColor || problem.holdColor || "").trim();
    const info = (editInfo || problem.info || "").trim();
    const assignedGrade = normalizeGrade(editGrade || problem.assignedGrade);
    if (!holdColor || !info) {
      toast.error("Hold color and notes are required to update this problem.");
      return;
    }

    try {
      setRemoving(true);
      const updated = await updateClimbingProblem(user, wallSectionId, problemId, {
        holdColor,
        info,
        assignedGrade,
        objectFileName: null,
        imageURL: null,
      });
      onProblemUpdated({
        holdColor: updated.holdColor ?? holdColor,
        info: updated.info ?? info,
        assignedGrade: updated.assignedGrade ?? assignedGrade,
        imageURL: null,
      });
      toast.success("Problem photo removed.");
    } catch (err) {
      console.error("Remove problem photo failed:", err);
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
          <DropdownMenuItem disabled={dialogBusy} onClick={openEditDialog}>
            Edit problem
          </DropdownMenuItem>
          {onDeleteProblem && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                variant="destructive"
                disabled={dialogBusy}
                onClick={onDeleteProblem}
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
          setEditOpen(open);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Edit problem</DialogTitle>
            <DialogDescription>
              Update this problem&apos;s details and photo.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSave} className="grid gap-4">
            <div className="grid gap-1.5">
              <label
                htmlFor={`edit-problem-color-${problemId}`}
                className="text-sm font-medium text-foreground"
              >
                Hold color
              </label>
              <input
                id={`edit-problem-color-${problemId}`}
                name="holdColor"
                type="text"
                autoComplete="off"
                required
                disabled={dialogBusy}
                value={editHoldColor}
                onChange={(ev) => setEditHoldColor(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
              />
            </div>

            <div className="grid gap-1.5">
              <label
                htmlFor={`edit-problem-grade-${problemId}`}
                className="text-sm font-medium text-foreground"
              >
                Assigned grade
              </label>
              <select
                id={`edit-problem-grade-${problemId}`}
                name="assignedGrade"
                required
                disabled={dialogBusy}
                value={editGrade}
                onChange={(ev) => setEditGrade(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
              >
                {GRADE_OPTIONS.map((grade) => (
                  <option key={grade} value={grade}>
                    {grade}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid gap-1.5">
              <label
                htmlFor={`edit-problem-info-${problemId}`}
                className="text-sm font-medium text-foreground"
              >
                Notes
              </label>
              <textarea
                id={`edit-problem-info-${problemId}`}
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
                  src={currentImageURL || PROBLEM_PLACEHOLDER_SRC}
                  alt={hasImage ? "" : "Default problem photo"}
                  className="aspect-[16/10] w-full object-cover"
                />
              </div>
              {!hasImage && (
                <p className="m-0 text-sm text-muted-foreground">
                  Using the default photo until you upload one.
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
                  {uploading
                    ? `Uploading… ${uploadProgress}%`
                    : hasImage
                      ? "Replace photo"
                      : "Upload photo"}
                </Button>
                {hasImage && (
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
                {saveSubmitting ? "Saving…" : "Save changes"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
