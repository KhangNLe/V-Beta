'use client';

import {
  addWallSection,
  deleteWallSection,
  fetchWallSectionsForUser,
} from '@/api/wallSections';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import GuestBanner from "@/components/GuestBanner";
import WallSectionAdminMenu from "@/components/WallSectionAdminMenu";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getAccountRole } from "@/lib/accountSession";
import {
  isAllowedWallImageFile,
  prepareWallImageFile,
  uploadWallSectionImage,
  WALL_IMAGE_ACCEPT,
} from "@/api/socialImage";
import { buttons, card, colors, fontFamily, layout } from "@/ui/appTheme";
import { ChevronDown } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "react-toastify";

const WALL_SECTION_PLACEHOLDER_SRC = "/co-op.png";

/** @param {{ imageURL?: string | null, imageUrl?: string | null }} section */
function sectionImageURL(section) {
  return section?.imageURL ?? section?.imageUrl ?? null;
}

const GYM_INFO = {
  name: "Minnesota Climbing Cooperative",
  description:
    "Volunteer-run bouldering gym in the Thorp Building in NE Minneapolis. The only bouldering co-op in the Twin Cities metro that run by climbers, for climbers. All proceeds go back into making the Co-op better for everyone who climbs here.",
  location: "1620 Central Ave NE, Suite 178, Minneapolis, MN 55413",
  hours:
    "Sundays 11am–3pm; Mondays 5:30pm–9:30pm; First Fridays 6pm–9pm. Access pass holders climb 24/7.",
  website: "https://www.mnclimbingcoop.com/",
};

export default function MainPage() {
  const router = useRouter();
  const { user, account, ready } = useRequireAuth({
    redirectMode: "push",
    requireAuth: false,
    requireEmailVerified: true,
  });
  const [sections, setSections] = useState([]);
  const isAdmin = getAccountRole(account).toUpperCase().includes('ADMIN');

  const [fetchError, setFetchError] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newSectionName, setNewSectionName] = useState('');
  const [newSectionInfo, setNewSectionInfo] = useState('');
  const [newSectionPhoto, setNewSectionPhoto] = useState(/** @type {File | null} */ (null));
  const [newSectionPhotoPreview, setNewSectionPhotoPreview] = useState(/** @type {string | null} */ (null));
  const [addUploadProgress, setAddUploadProgress] = useState(0);
  const [addSubmitting, setAddSubmitting] = useState(false);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const addPhotoInputRef = useRef(/** @type {HTMLInputElement | null} */ (null));
  const isSignedIn = !!user;

  const clearAddForm = useCallback(() => {
    setNewSectionName('');
    setNewSectionInfo('');
    setNewSectionPhoto(null);
    setNewSectionPhotoPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    setAddUploadProgress(0);
    if (addPhotoInputRef.current) addPhotoInputRef.current.value = '';
  }, []);

  const loadSections = useCallback(async (currentUser) => {
    try {
      const data = await fetchWallSectionsForUser(currentUser);
      setSections(Array.isArray(data) ? data : []);
      setFetchError(null);
    } catch (err) {
      console.error('Fetch wall sections failed:', err);
      setFetchError(err instanceof Error ? err.message : 'Unknown error');
    }
  }, []);

  useEffect(() => {
    if (!ready) return;
    void loadSections(user);
  }, [loadSections, user, ready]);

  const handleSelectSection = (section) => {
    router.push(`/wall/${section.wallSectionID}`);
  };

  const handleAddPhotoSelected = async (event) => {
    const input = event.target;
    const file = input.files?.[0] ?? null;
    if (!file) return;

    if (!isAllowedWallImageFile(file)) {
      toast.error(
        "Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).",
      );
      input.value = "";
      return;
    }

    try {
      const prepared = await prepareWallImageFile(file);
      setNewSectionPhotoPreview((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return URL.createObjectURL(prepared);
      });
      setNewSectionPhoto(prepared);
    } catch (err) {
      console.error("Prepare wall photo failed:", err);
      toast.error(
        err instanceof Error
          ? err.message
          : "Could not read that photo. Try another image.",
      );
      input.value = "";
    }
  };

  const handleClearAddPhoto = () => {
    setNewSectionPhoto(null);
    setNewSectionPhotoPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    if (addPhotoInputRef.current) addPhotoInputRef.current.value = '';
  };

  const handleAddSection = async (e) => {
    e.preventDefault();
    if (!isAdmin || !user || addSubmitting) return;

    const name = newSectionName.trim();
    const info = newSectionInfo.trim();
    if (!name || !info) {
      toast.error('Please enter both a name and description.');
      return;
    }

    try {
      setAddSubmitting(true);
      setAddUploadProgress(0);

      const created = await addWallSection(user, {
        wallSectionName: name,
        wallSectionInfo: info,
      });
      const createdId = created?.wallSectionID;

      if (newSectionPhoto) {
        if (createdId == null) {
          throw new Error('Wall section was created but no id was returned for photo upload.');
        }
        await uploadWallSectionImage(user, createdId, newSectionPhoto, {
          onProgress: setAddUploadProgress,
        });
      }

      await loadSections(user);
      clearAddForm();
      setAddOpen(false);
      toast.success(
        newSectionPhoto
          ? 'Wall section added with photo.'
          : 'Wall section added.',
      );
    } catch (err) {
      console.error('Add wall section failed:', err);
      // Section may already exist if photo upload failed after create — refresh list.
      try {
        await loadSections(user);
      } catch {
        /* ignore refresh error */
      }
    } finally {
      setAddSubmitting(false);
      setAddUploadProgress(0);
    }
  };

  const handleConfirmDelete = useCallback(async () => {
    if (!isAdmin) {
      setDeleteTarget(null);
      return;
    }
    if (!deleteTarget || !user || deleteSubmitting) return;
    const id = deleteTarget.wallSectionID;
    try {
      setDeleteSubmitting(true);
      await deleteWallSection(user, id);
      await loadSections(user);
      setDeleteTarget(null);
      toast.success('Wall section deleted.');
    } catch (err) {
      console.error('Delete wall section failed:', err);
      toast.error(
        err instanceof Error ? err.message : 'Failed to delete wall section.',
      );
    } finally {
      setDeleteSubmitting(false);
    }
  }, [deleteSubmitting, deleteTarget, isAdmin, loadSections, user]);

  const handleSectionUpdated = useCallback((wallSectionID, patch) => {
    setSections((prev) =>
      prev.map((section) =>
        section.wallSectionID === wallSectionID
          ? { ...section, ...patch }
          : section,
      ),
    );
  }, []);

  if (!ready) return <PageLoader message="Loading…" />;

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        {!isSignedIn && (
          <GuestBanner message="You are browsing as a guest. Sign in to manage your account and access all features." />
        )}
        <header className="mb-6">
          <h1 className="m-0 text-[1.75rem] font-bold" style={{ color: colors.text }}>
            {GYM_INFO.name}
          </h1>
        </header>

        {/* Gym Info Card */}
        <Card
          className="relative gap-0 overflow-hidden py-0 ring-0"
          style={{
            ...card.surface,
            position: "relative",
            overflow: "hidden",
            fontFamily,
            padding: "16px 22px 16px 20px",
            marginBottom: "28px",
          }}
        >
          <div style={card.accentBar} aria-hidden />
          <details>
            <summary
              className="flex cursor-pointer list-none items-center justify-between gap-3 marker:content-none [&::-webkit-details-marker]:hidden"
            >
              <CardTitle
                className="text-xs font-semibold tracking-wide uppercase"
                style={{ color: colors.subtle }}
              >
                Gym info
              </CardTitle>
              <ChevronDown
                className="size-4 shrink-0 transition-transform [[open]_&]:rotate-180"
                style={{ color: colors.subtle }}
                aria-hidden
              />
            </summary>
            <CardHeader className="rounded-none px-0 pt-3 pb-0">
              <CardDescription
                className="mb-3 max-w-[65ch] text-[0.9375rem] leading-[1.55]"
                style={{ color: colors.muted }}
              >
                {GYM_INFO.description}
              </CardDescription>
            </CardHeader>
            <CardContent className="px-0 pb-0">
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium" style={{ color: colors.muted }}>
                  <span style={{ color: colors.subtle }}>Location · </span>
                  {GYM_INFO.location}
                </span>
                <span className="text-sm font-medium" style={{ color: colors.muted }}>
                  <span style={{ color: colors.subtle }}>Open Hours · </span>
                  {GYM_INFO.hours}
                </span>
                <span className="text-sm font-medium" style={{ color: colors.muted }}>
                  <span style={{ color: colors.subtle }}>Website · </span>
                  <a
                    href={GYM_INFO.website}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="underline-offset-2 hover:underline"
                    style={{ color: colors.primary }}
                  >
                    mnclimbingcoop.com
                  </a>
                </span>
              </div>
            </CardContent>
          </details>
        </Card>

        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="m-0 text-lg font-semibold" style={{ color: colors.muted }}>
            Wall Sections
          </h2>

          {isAdmin && (
            <Button type="button" className="shrink-0" style={buttons.primary} onClick={() => setAddOpen(true)}>
              Add Wall Section
            </Button>
          )}
        </div>

        {fetchError && (
          <div
            className="mb-5 rounded-lg px-3.5 py-3"
            style={{
              color: colors.danger,
              background: colors.dangerBg,
              border: `1px solid ${colors.dangerBorder}`,
            }}
          >
            {fetchError}
          </div>
        )}

        {/* Wall sections grid or empty */}
        {sections.length === 0 ? (
          <p className="m-0" style={{ color: colors.subtle }}>
            No wall sections found.
          </p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(280px,1fr))]">
            {sections.map((section) => {
              const imageURL = sectionImageURL(section);
              return (
              <Card
                key={section.wallSectionID}
                className="gap-2.5 overflow-hidden p-0 py-0 ring-0"
                style={{
                  ...card.surface,
                  fontFamily,
                  position: "relative",
                }}
              >
                <div className="relative aspect-[16/10] w-full overflow-hidden bg-muted">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={imageURL || WALL_SECTION_PLACEHOLDER_SRC}
                    alt={imageURL ? "" : "Default wall section photo"}
                    className="h-full w-full object-cover"
                  />
                </div>

                <CardHeader className="px-5 pt-5 pb-0">
                  <CardTitle
                    className="min-w-0 text-lg font-semibold leading-[1.3]"
                    style={{ color: colors.text }}
                  >
                    {section.wallSectionName}
                  </CardTitle>
                  {isAdmin && user && (
                    <CardAction>
                      <WallSectionAdminMenu
                        user={user}
                        section={section}
                        onSectionUpdated={(patch) =>
                          handleSectionUpdated(section.wallSectionID, patch)
                        }
                        onDeleteSection={() => setDeleteTarget(section)}
                      />
                    </CardAction>
                  )}
                </CardHeader>

                <CardContent className="flex flex-grow flex-col px-5 pb-0 pt-0">
                  <p className="m-0 text-sm leading-normal" style={{ color: colors.muted }}>
                    {section.wallSectionInfo || "No description available for this section."}
                  </p>
                </CardContent>

                <CardFooter className="mt-1.5 flex w-full flex-col rounded-none border-border border-t bg-transparent px-5 py-4">
                  <Button
                    type="button"
                    className="w-full"
                    style={buttons.primary}
                    onClick={() => handleSelectSection(section)}
                  >
                    View section
                  </Button>
                </CardFooter>
              </Card>
              );
            })}
          </div>
        )}
      </div>

      {/* Add wall section dialog */}
      <Dialog
        open={addOpen}
        onOpenChange={(open) => {
          if (addSubmitting) return;
          setAddOpen(open);
          if (!open) clearAddForm();
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Wall Section</DialogTitle>
            <DialogDescription>
              Enter a name and description. You can optionally add a photo.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddSection} className="grid gap-4">
            <div className="grid gap-1.5">
              <label htmlFor="add-ws-name" className="text-sm font-medium text-foreground">
                Name
              </label>
              <input
                id="add-ws-name"
                name="name"
                type="text"
                autoComplete="off"
                required
                disabled={addSubmitting}
                value={newSectionName}
                onChange={(ev) => setNewSectionName(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
                placeholder="e.g. Bouldering Wall A"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-ws-info" className="text-sm font-medium text-foreground">
                Description
              </label>
              <textarea
                id="add-ws-info"
                name="info"
                rows={3}
                required
                disabled={addSubmitting}
                value={newSectionInfo}
                onChange={(ev) => setNewSectionInfo(ev.target.value)}
                className="resize-y rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
                placeholder="Short summary for climbers"
              />
            </div>

            <div className="grid gap-2">
              <span className="text-sm font-medium text-foreground">Photo (optional)</span>
              <div className="overflow-hidden rounded-md border border-border">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={newSectionPhotoPreview || WALL_SECTION_PLACEHOLDER_SRC}
                  alt={
                    newSectionPhotoPreview
                      ? "Selected wall section photo"
                      : "Default wall section photo"
                  }
                  className="aspect-[16/10] w-full object-cover"
                />
              </div>
              {!newSectionPhoto && (
                <p className="m-0 text-sm text-muted-foreground">
                  This default photo appears on the wall section until you upload one.
                </p>
              )}
              <input
                ref={addPhotoInputRef}
                type="file"
                accept={WALL_IMAGE_ACCEPT}
                className="hidden"
                aria-hidden
                tabIndex={-1}
                onChange={handleAddPhotoSelected}
              />
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={addSubmitting}
                  onClick={() => addPhotoInputRef.current?.click()}
                >
                  {newSectionPhoto ? "Change photo" : "Upload photo"}
                </Button>
                {newSectionPhoto && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={addSubmitting}
                    onClick={handleClearAddPhoto}
                  >
                    Remove photo
                  </Button>
                )}
              </div>
              {addSubmitting && newSectionPhoto && addUploadProgress > 0 && (
                <p className="m-0 text-xs text-muted-foreground" role="status">
                  Uploading photo… {addUploadProgress}%
                </p>
              )}
            </div>

            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={addSubmitting}
                onClick={() => setAddOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={addSubmitting} style={buttons.primary}>
                {addSubmitting
                  ? newSectionPhoto && addUploadProgress > 0
                    ? `Uploading… ${addUploadProgress}%`
                    : "Adding..."
                  : "Add section"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete wall section confirmation */}
      <AlertDialog
        open={deleteTarget != null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
      >
        <AlertDialogContent className="data-[size=default]:sm:max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle>{` Delete "${deleteTarget?.wallSectionName}"?`}</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="pt-4">
            <AlertDialogCancel type="button">Cancel</AlertDialogCancel>
            <AlertDialogAction
              type="button"
              variant="destructive"
              disabled={deleteSubmitting}
              onClick={handleConfirmDelete}
            >
              {deleteSubmitting ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}
