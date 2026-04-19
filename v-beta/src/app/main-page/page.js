"use client";

import {
  addWallSection,
  deleteWallSection,
  fetchWallSectionsForUser,
} from "@/api/wallSections";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { colors } from "@/ui/appTheme";
import { MoreVertical } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";

export default function MainPage() {
  const router = useRouter();
  const { user, account, ready } = useRequireAuth({ redirectMode: "push" });
  const [sections, setSections] = useState([]);
  const isAdmin = (account?.roleName || "").toUpperCase().includes("ADMIN");

  const [fetchError, setFetchError] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newSectionName, setNewSectionName] = useState("");
  const [newSectionInfo, setNewSectionInfo] = useState("");
  const [addSubmitting, setAddSubmitting] = useState(false);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const loadSections = useCallback(async (currentUser) => {
    try {
      const data = await fetchWallSectionsForUser(currentUser);
      setSections(Array.isArray(data) ? data : []);
      setFetchError(null);
    } catch (err) {
      console.error("Fetch wall sections failed:", err);
      setFetchError(err instanceof Error ? err.message : "Unknown error");
    }
  }, []);

  useEffect(() => {
    if (!user) return;
    void loadSections(user);
  }, [loadSections, user]);

  const handleSelectSection = (section) => {
    router.push(`/wall/${section.wallSectionID}`);
  };

  const handleAddSection = async (e) => {
    e.preventDefault();
    if (!isAdmin || !user || addSubmitting) return;

    const name = newSectionName.trim();
    const info = newSectionInfo.trim();
    if (!name || !info) {
      toast.error("Please enter both a name and description.");
      return;
    }

    try {
      setAddSubmitting(true);
      await addWallSection(user, {
        wallSectionName: name,
        wallSectionInfo: info,
      });
      await loadSections(user);
      setNewSectionName("");
      setNewSectionInfo("");
      setAddOpen(false);
      toast.success("Wall section added.");
    } catch (err) {
      console.error("Add wall section failed:", err);
      toast.error(err instanceof Error ? err.message : "Failed to add wall section.");
    } finally {
      setAddSubmitting(false);
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
      toast.success("Wall section deleted.");
    } catch (err) {
      console.error("Delete wall section failed:", err);
      toast.error(err instanceof Error ? err.message : "Failed to delete wall section.");
    } finally {
      setDeleteSubmitting(false);
    }
  }, [deleteSubmitting, deleteTarget, isAdmin, loadSections, user]);

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;
  
  return (
    <main className="min-h-screen bg-zinc-100 px-6 py-7 pb-12 font-sans text-zinc-900">
      <div className="mx-auto max-w-[960px]">
        <header className="mb-6">
          <h1 className="m-0 text-[1.75rem] font-bold">GYM</h1>
        </header>

        {/* Gym Info Card */}
        <Card className="relative mb-7 gap-0 overflow-hidden border border-zinc-200 bg-white py-[22px] pr-[22px] pl-5 shadow-sm ring-0">
          <div
            className="pointer-events-none absolute top-0 bottom-0 left-0 w-1 bg-linear-to-b from-blue-600 to-blue-700"
            aria-hidden
          />
          <CardHeader className="rounded-none px-0 pt-0 pb-0">
            <CardTitle className="mb-1.5 text-xs font-semibold tracking-wide text-zinc-500 uppercase">
              Gym info
            </CardTitle>
            <CardDescription className="mb-3 max-w-[65ch] text-[0.9375rem] leading-[1.55] text-zinc-600">
              A fantastic gym with a variety of climbing walls for all skill levels. Come in and climb!
            </CardDescription>
          </CardHeader>
          <CardContent className="px-0 pb-0">
            <div className="flex flex-wrap items-center gap-x-3.5 gap-y-2">
              <span className="text-sm font-medium text-zinc-600">
                <span className="text-zinc-500">Location · </span>
                123 Climbing St, Boulder City
              </span>
            </div>
          </CardContent>
        </Card>

        <div
          className="mb-4 flex flex-wrap items-center justify-between gap-3"
          style={{
            "--section-btn-primary": colors.primary,
            "--section-btn-primary-hover": colors.primaryDark,
          }}
        >
          <h2 className="m-0 text-lg font-bold text-zinc-900">Wall Sections</h2>

          {isAdmin && (
            <Button
              type="button"
              className="shrink-0 border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
              onClick={() => setAddOpen(true)}
            >
              Add Wall Section
            </Button>
          )}
        </div>

        {fetchError && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-3.5 py-3 text-red-700">
            {fetchError}
          </div>
        )}

        {/* Wall sections grid or empty */}
        {sections.length === 0 ? (
          <p className="m-0 text-zinc-500">No wall sections found.</p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(280px,1fr))]">
            {sections.map((section) => (
              <Card
                key={section.wallSectionID}
                className="gap-2.5 overflow-hidden border border-zinc-200 bg-white p-0 py-5 shadow-sm ring-0"
                style={{
                  "--section-btn-primary": colors.primary,
                  "--section-btn-primary-hover": colors.primaryDark,
                }}
              >
                <CardHeader className="px-5 pt-0 pb-0">
                  <CardTitle className="min-w-0 text-lg font-semibold leading-[1.3] text-zinc-900">
                    {section.wallSectionName}
                  </CardTitle>
                  {isAdmin && (
                    <CardAction>
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          render={
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-sm"
                              className="shrink-0 text-zinc-600"
                              aria-label="Section actions"
                            />
                          }
                        >
                          <MoreVertical className="size-4" />
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            variant="destructive"
                            onClick={() => setDeleteTarget(section)}
                          >
                            Delete
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </CardAction>
                  )}
                </CardHeader>

                <CardContent className="flex flex-grow flex-col px-5 pb-0 pt-0">
                  <p className="m-0 text-sm leading-normal text-zinc-600">
                    {section.wallSectionInfo || "No description available for this section."}
                  </p>
                </CardContent>

                <CardFooter className="mt-1.5 flex w-full flex-col rounded-none border-t border-zinc-200 px-5 py-4">
                  <Button
                    type="button"
                    className="w-full border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
                    onClick={() => handleSelectSection(section)}
                  >
                    View section
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Add wall section dialog */}
      <Dialog
        open={addOpen}
        onOpenChange={(open) => {
          setAddOpen(open);
          if (!open) {
            setNewSectionName("");
            setNewSectionInfo("");
          }
        }}
      >
        <DialogContent
          className="sm:max-w-md"
          style={{
            "--section-btn-primary": colors.primary,
            "--section-btn-primary-hover": colors.primaryDark,
          }}
        >
          <DialogHeader>
            <DialogTitle>Add Wall Section</DialogTitle>
            <DialogDescription>
              Enter a name and description for this section.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddSection} className="grid gap-3">
            <div className="grid gap-1.5">
              <label htmlFor="add-ws-name" className="text-sm font-medium text-zinc-700">
                Name
              </label>
              <input
                id="add-ws-name"
                name="name"
                type="text"
                autoComplete="off"
                required
                value={newSectionName}
                onChange={(ev) => setNewSectionName(ev.target.value)}
                className="rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-200"
                placeholder="e.g. Bouldering Wall A"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-ws-info" className="text-sm font-medium text-zinc-700">
                Description
              </label>
              <textarea
                id="add-ws-info"
                name="info"
                rows={3}
                required
                value={newSectionInfo}
                onChange={(ev) => setNewSectionInfo(ev.target.value)}
                className="resize-y rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-200"
                placeholder="Short summary for climbers"
              />
            </div>
            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button type="button" variant="outline" onClick={() => setAddOpen(false)}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={addSubmitting}
                className="border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
              >
                {addSubmitting ? "Adding..." : "Add section"}
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
              {deleteSubmitting ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}
