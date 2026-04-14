"use client";

import { fetchWallSectionProblemsForUser, fetchWallSectionsForUser } from "@/api/wallSections";
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
import PageLoader from "@/components/ui/PageLoader";
import { Button } from "@/components/ui/button";
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
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { MoreVertical } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";

export default function WallSectionPage() {
  const router = useRouter();
  const params = useParams();
  const { user, ready } = useRequireAuth({ redirectMode: "push" });

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

  const rawWallSectionID = params?.wallSectionID;
  const wallSectionID = useMemo(() => {
    const normalized = Array.isArray(rawWallSectionID) ? rawWallSectionID[0] : rawWallSectionID;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawWallSectionID]);

  useEffect(() => {
    if (!ready || !user) return;
    if (!wallSectionID) {
      setLoading(false);
      setFetchError("Invalid wall section id.");
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        const [sectionsData, problemsData] = await Promise.all([
          fetchWallSectionsForUser(user),
          fetchWallSectionProblemsForUser(user, wallSectionID),
        ]);
        if (cancelled) return;

        const selected = sectionsData.find((item) => item.wallSectionID === wallSectionID) || null;
        setSection(selected);
        setProblems(Array.isArray(problemsData) ? problemsData : []);
        setFetchError(null);
      } catch (err) {
        console.error("Failed to fetch wall section page data:", err);
        if (!cancelled) setFetchError(err instanceof Error ? err.message : "Unknown error");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [ready, user, wallSectionID]);

  const handleViewProblem = (problemId) => {
    router.push(`/wall/${wallSectionID}/problem/${problemId}`);
  };

  const handleBackToSections = () => {
    router.push("/main-page");
  };

  const handleConfirmDelete = useCallback(() => {
    if (!deleteTarget) return;
    const targetId = deleteTarget.problemId;
    setProblems((prev) => prev.filter((problem) => problem.problemId !== targetId));
    setDeleteTarget(null);
    toast.success("Problem deleted.");
  }, [deleteTarget]);

  const handleAddProblem = (e) => {
    e.preventDefault();
    const holdColor = newHoldColor.trim();
    const assignedGrade = newAssignedGrade.trim();
    const problemInfo = newProblemInfo.trim();
    const nextId =
      problems.length === 0
        ? 1
        : Math.max(...problems.map((problem) => Number(problem.problemId) || 0)) + 1;

    setProblems((prev) => [
      ...prev,
      {
        problemId: nextId,
        holdColor,
        assignedGrade,
        problemInfo,
      },
    ]);
    setNewHoldColor("");
    setNewAssignedGrade("");
    setNewProblemInfo("");
    setAddOpen(false);
    toast.success("Problem added.");
  };

  const handleResetWallSection = useCallback(() => {
    setProblems([]);
    setResetOpen(false);
    toast.success("Wall section reset.");
  }, []);

  if (!ready) return <PageLoader message="Loading…" />;
  if (!user) return <PageLoader message="Redirecting…" />;
  if (loading) return <PageLoader message="Loading wall section…" />;

  return (
    <main className="min-h-screen bg-zinc-100 px-6 py-7 pb-12 text-zinc-900">
      <div className="mx-auto max-w-[960px]">
        <Button
          type="button"
          variant="outline"
          onClick={handleBackToSections}
          className="mb-4"
        >
          Back
        </Button>

        {/* Section Header Card */}
        <section>
          <Card className="relative mb-7 gap-0 overflow-hidden border border-zinc-200 bg-white py-[22px] pr-[22px] pl-5 shadow-sm ring-0">
            <div
              className="pointer-events-none absolute top-0 bottom-0 left-0 w-1 bg-linear-to-b from-blue-600 to-blue-700"
              aria-hidden
            />
            <CardHeader className="rounded-none px-0 pt-0 pb-0">
              <CardTitle className="m-0 text-[1.75rem] font-bold text-zinc-900">
                {section?.wallSectionName || `Section ${wallSectionID}`}
              </CardTitle>
              <CardDescription className="mt-2 max-w-[65ch] text-[0.9375rem] leading-[1.55] text-zinc-600">
                {section?.wallSectionInfo || "No section description available."}
              </CardDescription>
            </CardHeader>
          </Card>
        </section>

        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="m-0 text-lg font-bold text-zinc-900">Problems</h2>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="destructive"
              className="shrink-0"
              onClick={() => setResetOpen(true)}
            >
              Reset Wall Section
            </Button>
            <Button
              type="button"
              className="shrink-0 border-transparent bg-blue-600 text-white hover:bg-blue-700"
              onClick={() => setAddOpen(true)}
            >
              Add New Problem
            </Button>
          </div>
        </div>

        {fetchError && (
          <Card className="mb-5 gap-0 border-destructive/40 bg-destructive/8 py-0 text-destructive ring-0">
            <CardContent className="px-3.5 py-3">{fetchError}</CardContent>
          </Card>
        )}

        {problems.length === 0 ? (
          <p className="m-0 text-zinc-500">No problems found for this wall section.</p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(260px,1fr))]">
            {problems.map((problem) => (
              <article key={problem.problemId}>
                <Card className="gap-2.5 overflow-hidden border border-zinc-200 bg-white p-0 py-5 shadow-sm ring-0">
                  <CardHeader className="px-5 pt-0 pb-0">
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle className="text-lg font-semibold leading-[1.35] text-zinc-900">
                        {problem.holdColor}
                      </CardTitle>
                      <CardAction>
                        <DropdownMenu>
                          <DropdownMenuTrigger
                            render={
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon-sm"
                                className="shrink-0 text-zinc-600"
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
                    </div>
                  </CardHeader>

                  <CardContent className="flex flex-grow flex-col px-5 pb-0 pt-0">
                    <p className="m-0 text-sm leading-6 text-zinc-600">
                      {problem.info || "No problem notes available."}
                    </p>
                  </CardContent>

                  <CardFooter className="mt-1.5 flex w-full flex-col rounded-none border-t border-zinc-200 px-5 py-4">
                    <Button
                      type="button"
                      onClick={() => handleViewProblem(problem.problemId)}
                      className="w-full border-transparent bg-blue-600 text-white hover:bg-blue-700"
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
              Enter problem details for this wall section.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddProblem} className="grid gap-3">
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-hold-color" className="text-sm font-medium text-zinc-700">
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
                className="rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-200"
                placeholder="e.g. Blue"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-grade" className="text-sm font-medium text-zinc-700">
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
                className="rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-200"
                placeholder="e.g. V4"
              />
            </div>
            <div className="grid gap-1.5">
              <label htmlFor="add-problem-info" className="text-sm font-medium text-zinc-700">
                Notes
              </label>
              <textarea
                id="add-problem-info"
                name="problemInfo"
                rows={3}
                required
                value={newProblemInfo}
                onChange={(ev) => setNewProblemInfo(ev.target.value)}
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
                className="border-transparent bg-blue-600 text-white hover:bg-blue-700"
              >
                Add problem
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={resetOpen}
        onOpenChange={(open) => {
          setResetOpen(open);
        }}
      >
        <AlertDialogContent className="data-[size=default]:sm:max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle>Reset this wall section?</AlertDialogTitle>
            <AlertDialogDescription>
              This will remove all problems from this wall section and cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="pt-4">
            <AlertDialogCancel type="button">Cancel</AlertDialogCancel>
            <AlertDialogAction
              type="button"
              variant="destructive"
              onClick={handleResetWallSection}
            >
              Reset
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

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