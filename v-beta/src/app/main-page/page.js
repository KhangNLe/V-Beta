"use client";

import { fetchWallSectionsForUser } from "@/api/wallSections";
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
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { colors } from "@/ui/appTheme";
import { MoreVertical } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";

export default function MainPage() {
  const router = useRouter();
  const { user, ready } = useRequireAuth({ redirectMode: "push" });
  const [sections, setSections] = useState([]);
  const [fetchError, setFetchError] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newSectionName, setNewSectionName] = useState("");
  const [newSectionInfo, setNewSectionInfo] = useState("");

  useEffect(() => {
    if (!user) return;

    let cancelled = false;
    (async () => {
      try {
        const data = await fetchWallSectionsForUser(user);
        if (!cancelled) {
          setSections(data);
          setFetchError(null);
        }
      } catch (err) {
        console.error("Fetch wall sections failed:", err);
        if (!cancelled) {
          setFetchError(err instanceof Error ? err.message : "Unknown error");
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [user]);

  const handleSelectSection = (section) => {
    router.push(`/wall/${section.wallSectionID}`);
  };

  // TODO: replace client-side ID generation with real ID from API response
  const handleAddSection = (e) => {
    e.preventDefault();
    const name = newSectionName.trim();
    const info = newSectionInfo.trim();

    // Temporary fake ID
    const nextId =
      sections.length === 0
        ? 1
        : Math.max(...sections.map((s) => s.wallSectionID)) + 1;

    setSections((prev) => [
      ...prev,
      {
        wallSectionID: nextId,
        wallSectionName: name,
        wallSectionInfo: info,
      },
    ]);

    setNewSectionName("");
    setNewSectionInfo("");
    setAddOpen(false);
    toast.success("Wall section added.");
  };

  // TODO: send delete request to API to remove section from database
  const handleConfirmDelete = useCallback(() => {
    if (!deleteTarget) return;
    const id = deleteTarget.wallSectionID;

    setSections((prev) => prev.filter((s) => s.wallSectionID !== id));
    setDeleteTarget(null);
    toast.success("Wall section deleted.");
  }, [deleteTarget]);

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;
  
  return (
    <main className="min-h-screen bg-zinc-100 px-6 py-7 pb-12 font-sans text-zinc-900">
      <div className="mx-auto max-w-[960px]">
        <header className="mb-6">
          <h1 className="m-0 text-[1.75rem] font-bold">GYM</h1>
        </header>

        {/* Gym Info Card */}
        <section className="relative mb-7 overflow-hidden rounded-xl border border-zinc-200 bg-white py-[22px] pr-[22px] pl-5 shadow-sm">
          <div
            className="pointer-events-none absolute top-0 bottom-0 left-0 w-1 bg-linear-to-b from-blue-600 to-blue-700"
            aria-hidden
          />
          <p className="mb-1.5 text-xs font-semibold tracking-wide text-zinc-500 uppercase">
            Gym info
          </p>
          <p className="mb-3 max-w-[65ch] text-[0.9375rem] leading-[1.55] text-zinc-600">
            A fantastic gym with a variety of climbing walls for all skill levels. Come in and climb!
          </p>
          <div className="flex flex-wrap items-center gap-x-3.5 gap-y-2">
            <span className="text-sm font-medium text-zinc-600">
              <span className="text-zinc-500">Location · </span>
              123 Climbing St, Boulder City
            </span>
          </div>
        </section>

        <div
          className="mb-4 flex flex-wrap items-center justify-between gap-3"
          style={{
            "--section-btn-primary": colors.primary,
            "--section-btn-primary-hover": colors.primaryDark,
          }}
        >
          <h2 className="m-0 text-lg font-bold text-zinc-900">Wall Sections</h2>

          {/* TODO: hide if not admin */}
          <Button
            type="button"
            className="shrink-0 border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
            onClick={() => setAddOpen(true)}
          >
            Add Wall Section
          </Button>
        </div>

        {fetchError && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 px-3.5 py-3 text-red-700">
            {fetchError}
          </div>
        )}

        {/* Wall sections grid or empty state */}
        {sections.length === 0 ? (
          <p className="m-0 text-zinc-500">No wall sections found.</p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(280px,1fr))]">
            {sections.map((section) => (
              <article
                key={section.wallSectionID}
                className="flex flex-col gap-2.5 rounded-xl border border-zinc-200 bg-white p-5 shadow-sm"
                style={{
                  "--section-btn-primary": colors.primary,
                  "--section-btn-primary-hover": colors.primaryDark,
                }}
              >
                {/* Wall section card */}
                <div className="flex items-start justify-between gap-2">
                  <h3 className="m-0 min-w-0 flex-1 text-lg font-semibold leading-[1.3] text-zinc-900">
                    {section.wallSectionName}
                  </h3>
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
                </div>

                <p className="m-0 flex-grow text-sm leading-normal text-zinc-600">
                  {section.wallSectionInfo || "No description available for this section."}
                </p>

                <Button
                  type="button"
                  className="mt-1.5 self-start border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
                  onClick={() => handleSelectSection(section)}
                >
                  View section
                </Button>
              </article>
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
                className="border-transparent bg-[var(--section-btn-primary)] text-white hover:bg-[var(--section-btn-primary-hover)]"
              >
                Add section
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
            <AlertDialogTitle> Delete "{deleteTarget?.wallSectionName}"?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="pt-4">
            <AlertDialogCancel type="button">Cancel</AlertDialogCancel>
            <AlertDialogAction
              type="button"
              variant="destructive"
              onClick={handleConfirmDelete}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}
