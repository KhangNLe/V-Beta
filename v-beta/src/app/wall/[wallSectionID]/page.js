"use client";

import {
  createWallSectionProblem,
  deleteWallSectionProblem,
  fetchWallSectionProblemsForUser,
  fetchWallSectionsForUser,
  resetWallSection,
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
import GuestBanner from "@/components/GuestBanner";
import PageLoader from "@/components/ui/PageLoader";
import { Button } from "@/components/ui/button";
import { buttons, card, colors, fontFamily, layout } from "@/ui/appTheme";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
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
import { ArrowLeftIcon, MoreVertical } from "lucide-react";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";

/** @param {string} raw */
function assignedGradeToEnum(raw) {
  const t = raw.trim().toUpperCase();
  if (t === "VB") return "VB";
  const match = /^V(\d+)$/.exec(t);
  if (match) {
    const n = parseInt(match[1], 10);
    if (n >= 0 && n <= 17) return `V${n}`;
  }
  return null;
}

export default function WallSectionPage() {
  const router = useRouter();
  const params = useParams();
  const { user, account, ready } = useRequireAuth({
    redirectMode: "push",
    requireAuth: false,
    requireEmailVerified: true,
  });
  const [section, setSection] = useState(null);
  const [problems, setProblems] = useState([]);
  const [fetchError, setFetchError] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [resetOpen, setResetOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [newHoldColor, setNewHoldColor] = useState("");
  const [newAssignedGrade, setNewAssignedGrade] = useState("");
  const [newProblemInfo, setNewProblemInfo] = useState("");
  const [loading, setLoading] = useState(true);
  const [addSubmitting, setAddSubmitting] = useState(false);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [resetSubmitting, setResetSubmitting] = useState(false);

  const isSignedIn = !!user;
  const canManageWallProblems = useMemo(() => {
    const roleUpper = (account?.roleName || "").toUpperCase();
    return roleUpper.includes("SETTER");
  }, [account?.roleName]);

  const rawWallSectionID = params?.wallSectionID;
  const wallSectionID = useMemo(() => {
    const normalized = Array.isArray(rawWallSectionID) ? rawWallSectionID[0] : rawWallSectionID;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawWallSectionID]);

  const loadProblems = useCallback(
    async (currentUser) => {
      if (!wallSectionID) return;
      try {
        const problemsData = await fetchWallSectionProblemsForUser(currentUser, wallSectionID);
        setProblems(Array.isArray(problemsData) ? problemsData : []);
      } catch (err) {
        console.error("Failed to fetch wall section problems:", err);
        const message =
          err instanceof Error ? err.message : "Failed to load problems for this section.";
        toast.error(message);
        setProblems([]);
      }
    },
    [wallSectionID],
  );

  useEffect(() => {
    if (!ready) return;
    if (!wallSectionID) {
      setSection(null);
      setProblems([]);
      setLoading(false);
      setFetchError("Invalid wall section id.");
      return;
    }

    let cancelled = false;
    (async () => {
      let redirectingUnknownWall = false;
      try {
        setLoading(true);
        setSection(null);
        setProblems([]);
        setFetchError(null);

        const sectionsData = await fetchWallSectionsForUser(user);
        if (cancelled) return;

        const selected =
          sectionsData.find((item) => item.wallSectionID === wallSectionID) || null;
        if (!selected) {
          if (!cancelled) {
            toast.error("That wall section does not exist.");
            router.replace("/main-page");
            redirectingUnknownWall = true;
          }
          return;
        }

        setSection(selected);

        await loadProblems(user);
        if (cancelled) return;

        setFetchError(null);
      } catch (err) {
        console.error("Failed to fetch wall section page data:", err);
        if (!cancelled) setFetchError(err instanceof Error ? err.message : "Unknown error");
      } finally {
        if (!cancelled && !redirectingUnknownWall) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [ready, user, wallSectionID, router, loadProblems]);

  const handleViewProblem = (problemId) => {
    router.push(`/wall/${wallSectionID}/problem/${problemId}`);
  };

  const handleBackToSections = () => {
    router.push("/main-page");
  };

  const handleConfirmDelete = useCallback(async () => {
    if (!canManageWallProblems || !user || !deleteTarget || !wallSectionID || deleteSubmitting) return;
    try {
      setDeleteSubmitting(true);
      const list = await deleteWallSectionProblem(user, wallSectionID, deleteTarget.problemId);
      setProblems(list);
      setDeleteTarget(null);
      toast.success("Problem deleted.");
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to delete problem.");
    } finally {
      setDeleteSubmitting(false);
    }
  }, [
    canManageWallProblems,
    user,
    deleteTarget,
    wallSectionID,
    deleteSubmitting,
  ]);

  const handleAddProblem = async (e) => {
    e.preventDefault();
    if (!canManageWallProblems || !user || addSubmitting || !wallSectionID) return;

    const holdColor = newHoldColor.trim();
    const assignedGradeEnum = assignedGradeToEnum(newAssignedGrade);
    const info = newProblemInfo.trim();
    if (!assignedGradeEnum) {
      toast.error("Enter a valid grade: VB or V0 through V17.");
      return;
    }

    try {
      setAddSubmitting(true);
      await createWallSectionProblem(user, wallSectionID, {
        holdColor,
        info,
        assignedGrade: assignedGradeEnum,
      });
      await loadProblems(user);
      setNewHoldColor("");
      setNewAssignedGrade("");
      setNewProblemInfo("");
      setAddOpen(false);
      toast.success("Problem added.");
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to add problem.");
    } finally {
      setAddSubmitting(false);
    }
  };

  const handleResetWallSection = useCallback(async () => {
    if (!canManageWallProblems || !user || !wallSectionID || resetSubmitting) return;
    try {
      setResetSubmitting(true);
      await resetWallSection(user, wallSectionID);
      await loadProblems(user);
      setResetOpen(false);
      toast.success("Wall section reset.");
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to reset wall section.");
    } finally {
      setResetSubmitting(false);
    }
  }, [canManageWallProblems, user, wallSectionID, resetSubmitting, loadProblems]);

  if (!ready) return <PageLoader message="Loading…" />;
  if (loading) return <PageLoader message="Loading wall section…" />;

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>
        {!isSignedIn && (
          <GuestBanner message="You are viewing this wall section as a guest. Sign in to unlock interactive features." />
        )}
        <Button
          type="button"
          variant="ghost"
          onClick={handleBackToSections}
          className="mb-4 text-muted-foreground hover:text-foreground"
        >
          <ArrowLeftIcon className="size-4" />
        </Button>

        {/* Section Header Card */}
        <section>
          <Card
            className="relative mb-7 gap-0 overflow-hidden py-0 ring-0"
            style={{
              ...card.surface,
              position: "relative",
              overflow: "hidden",
              fontFamily,
              padding: "22px 22px 22px 20px",
            }}
          >
            <div style={card.accentBar} aria-hidden />
            <CardHeader className="rounded-none px-0 pt-0 pb-0">
              <CardTitle className="m-0 text-[1.75rem] font-bold" style={{ color: colors.text }}>
                {section?.wallSectionName || `Section ${wallSectionID}`}
              </CardTitle>
              <CardDescription
                className="mt-2 max-w-[65ch] text-[0.9375rem] leading-[1.55]"
                style={{ color: colors.muted }}
              >
                {section?.wallSectionInfo || "No section description available."}
              </CardDescription>
            </CardHeader>
          </Card>
        </section>

        {/* Problems heading + actions */}
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="m-0 text-lg font-semibold" style={{ color: colors.muted }}>
            Problems
          </h2>
          {canManageWallProblems && (
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="destructive"
                className="shrink-0"
                onClick={() => setResetOpen(true)}
              >
                Reset Wall Section
              </Button>
              <Button type="button" className="shrink-0" style={buttons.primary} onClick={() => setAddOpen(true)}>
                Add New Problem
              </Button>
            </div>
          )}
        </div>

        {/* Fetch error */}
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

        {/* Problems grid or empty */}
        {problems.length === 0 ? (
          <p className="m-0" style={{ color: colors.subtle }}>
            No problems found for this wall section.
          </p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(260px,1fr))]">
            {problems.map((problem) => (
              <article key={problem.problemId}>
                <Card
                  className="gap-2.5 overflow-hidden p-0 py-5 ring-0"
                  style={{
                    ...card.surface,
                    fontFamily,
                    position: "relative",
                  }}
                >
                  <CardHeader className="px-5 pt-0 pb-0">
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle
                        className="min-w-0 text-lg font-semibold leading-[1.35]"
                        style={{ color: colors.text }}
                      >
                        {problem.holdColor}
                      </CardTitle>
                      {canManageWallProblems && (
                        <CardAction>
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              render={
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon-sm"
                                  className="shrink-0 text-muted-foreground"
                                  aria-label="Problem actions"
                                />
                              }
                            >
                              <MoreVertical className="size-4" />
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                variant="destructive"
                                onClick={() => setDeleteTarget(problem)}
                              >
                                Delete
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </CardAction>
                      )}
                    </div>
                  </CardHeader>

                  {/* Problem description */}
                  <CardContent className="flex flex-grow flex-col px-5 pb-0 pt-0">
                    <p className="m-0 text-sm leading-6" style={{ color: colors.muted }}>
                      {problem.info || "No problem notes available."}
                    </p>
                  </CardContent>

                  {/* Primary action */}
                  <CardFooter className="mt-1.5 flex w-full flex-col rounded-none border-border border-t bg-transparent px-5 py-4">
                    <Button
                      type="button"
                      className="w-full"
                      style={buttons.primary}
                      onClick={() => handleViewProblem(problem.problemId)}
                    >
                      View problem
                    </Button>
                  </CardFooter>
                </Card>
              </article>
            ))}
          </div>
        )}
      </div>

      {/* Add problem dialog */}
      <Dialog
        open={addOpen}
        onOpenChange={(open) => {
          setAddOpen(open);
          if (!open) {
            setNewHoldColor("");
            setNewAssignedGrade("");
            setNewProblemInfo("");
          }
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Problem</DialogTitle>
            <DialogDescription>
              Enter problem details for this wall section. Grade must be VB or V0 through V17 (e.g. V4).
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddProblem} className="grid gap-3">
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-hold-color" className="text-sm font-medium text-foreground">
                Hold Color
              </label>
              <input
                id="add-problem-hold-color"
                name="holdColor"
                type="text"
                autoComplete="off"
                required
                value={newHoldColor}
                onChange={(ev) => setNewHoldColor(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                placeholder="e.g. Blue"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-grade" className="text-sm font-medium text-foreground">
                Assigned Grade
              </label>
              <input
                id="add-problem-grade"
                name="assignedGrade"
                type="text"
                autoComplete="off"
                required
                value={newAssignedGrade}
                onChange={(ev) => setNewAssignedGrade(ev.target.value)}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                placeholder="e.g. V4"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-info" className="text-sm font-medium text-foreground">
                Notes
              </label>
              <textarea
                id="add-problem-info"
                name="problemInfo"
                rows={3}
                required
                value={newProblemInfo}
                onChange={(ev) => setNewProblemInfo(ev.target.value)}
                className="resize-y rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                placeholder="Short summary for climbers"
              />
            </div>
            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button type="button" variant="outline" onClick={() => setAddOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={addSubmitting} style={buttons.primary}>
                {addSubmitting ? "Adding…" : "Add problem"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Reset wall section confirmation */}
      <AlertDialog
        open={resetOpen}
        onOpenChange={(open) => {
          setResetOpen(open);
        }}
      >
        <AlertDialogContent className="data-[size=default]:sm:max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle>{`Reset "${section?.wallSectionName || `Section ${wallSectionID}`}"?`}</AlertDialogTitle>
            <AlertDialogDescription className="text-left sm:text-left [text-wrap:wrap] md:[text-wrap:wrap]">
              This will remove all problems from this wall section and cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="pt-4">
            <AlertDialogCancel type="button">Cancel</AlertDialogCancel>
            <AlertDialogAction
              type="button"
              variant="destructive"
              disabled={resetSubmitting}
              onClick={handleResetWallSection}
            >
              {resetSubmitting ? "Resetting…" : "Reset"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete problem confirmation */}
      <AlertDialog
        open={deleteTarget != null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
      >
        <AlertDialogContent className="data-[size=default]:sm:max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle>{`Delete "${deleteTarget?.holdColor || "problem"}"?`}</AlertDialogTitle>
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
              {deleteSubmitting ? "Deleting…" : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}