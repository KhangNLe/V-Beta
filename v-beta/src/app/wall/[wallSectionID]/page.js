"use client";

import {
  createWallSectionProblem,
  deleteWallSectionProblem,
  fetchFilteredWallSectionProblems,
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
import {
  isAllowedWallImageFile,
  prepareWallImageFile,
  uploadClimbingProblemImage,
  WALL_IMAGE_ACCEPT,
} from "@/api/socialImage";
import ClimbingProblemSetterMenu from "@/components/ClimbingProblemSetterMenu";
import GuestBanner from "@/components/GuestBanner";
import WallSectionAdminMenu from "@/components/WallSectionAdminMenu";
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
import { ArrowLeftIcon, SlidersHorizontal } from "lucide-react";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getAccountRole } from "@/lib/accountSession";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { toast } from "react-toastify";

const WALL_SECTION_PLACEHOLDER_SRC = "/co-op.png";
const PROBLEM_PLACEHOLDER_SRC = "/problem-holder.jpg";

/** @param {{ imageURL?: string | null, imageUrl?: string | null } | null} section */
function sectionImageURL(section) {
  return section?.imageURL ?? section?.imageUrl ?? null;
}

/** @param {{ imageURL?: string | null, imageUrl?: string | null } | null | undefined} problem */
function problemImageURL(problem) {
  return problem?.imageURL ?? problem?.imageUrl ?? null;
}

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

/** @param {string} grade */
function gradeIndex(grade) {
  return GRADE_OPTIONS.indexOf(grade);
}

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
  const [filterOpen, setFilterOpen] = useState(false);
  const [draftMinGrade, setDraftMinGrade] = useState("VB");
  const [draftMaxGrade, setDraftMaxGrade] = useState("V17");
  const [draftSortMode, setDraftSortMode] = useState("recent");
  const [appliedMinGrade, setAppliedMinGrade] = useState("VB");
  const [appliedMaxGrade, setAppliedMaxGrade] = useState("V17");
  const [appliedSortMode, setAppliedSortMode] = useState("recent");
  const [filtersActive, setFiltersActive] = useState(false);
  const [filterSubmitting, setFilterSubmitting] = useState(false);
  const [newHoldColor, setNewHoldColor] = useState("");
  const [newAssignedGrade, setNewAssignedGrade] = useState("VB");
  const [newProblemInfo, setNewProblemInfo] = useState("");
  const [newProblemPhoto, setNewProblemPhoto] = useState(null);
  const [newProblemPhotoPreview, setNewProblemPhotoPreview] = useState(null);
  const [addUploadProgress, setAddUploadProgress] = useState(0);
  const [convertingHeic, setConvertingHeic] = useState(false);
  const addPhotoInputRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [addSubmitting, setAddSubmitting] = useState(false);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [resetSubmitting, setResetSubmitting] = useState(false);

  const isSignedIn = !!user;
  const canManageWallProblems = useMemo(() => {
    const roleUpper = getAccountRole(account).toUpperCase();
    return roleUpper.includes("SETTER");
  }, [account]);
  const isAdmin = useMemo(() => {
    return getAccountRole(account).toUpperCase().includes("ADMIN");
  }, [account]);

  const handleSectionUpdated = useCallback((patch) => {
    setSection((prev) => (prev ? { ...prev, ...patch } : prev));
  }, []);

  const handleProblemUpdated = useCallback((problemId, patch) => {
    setProblems((prev) =>
      prev.map((item) => (item.problemId === problemId ? { ...item, ...patch } : item)),
    );
  }, []);

  const clearAddPhoto = () => {
    setNewProblemPhoto(null);
    setNewProblemPhotoPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    if (addPhotoInputRef.current) addPhotoInputRef.current.value = "";
  };

  const clearAddProblemForm = () => {
    setNewHoldColor("");
    setNewAssignedGrade("VB");
    setNewProblemInfo("");
    clearAddPhoto();
  };

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

  const loadFilteredProblems = useCallback(
    async (currentUser, min, max, sortMode) => {
      if (!wallSectionID) return;
      /** @type {{ min: string, max: string, sort?: "asc" | "desc" }} */
      const options = { min, max };
      if (sortMode === "easiest") options.sort = "asc";
      if (sortMode === "hardest") options.sort = "desc";

      let problemsData = await fetchFilteredWallSectionProblems(
        currentUser,
        wallSectionID,
        options,
      );
      problemsData = Array.isArray(problemsData) ? problemsData : [];

      if (sortMode === "recent") {
        problemsData = [...problemsData].sort((a, b) => {
          const aTime = Date.parse(a.createdDate || "") || 0;
          const bTime = Date.parse(b.createdDate || "") || 0;
          return bTime - aTime;
        });
      }

      setProblems(problemsData);
    },
    [wallSectionID],
  );

  const reloadProblems = useCallback(
    async (currentUser) => {
      if (filtersActive) {
        await loadFilteredProblems(
          currentUser,
          appliedMinGrade,
          appliedMaxGrade,
          appliedSortMode,
        );
      } else {
        await loadProblems(currentUser);
      }
    },
    [
      filtersActive,
      loadFilteredProblems,
      loadProblems,
      appliedMinGrade,
      appliedMaxGrade,
      appliedSortMode,
    ],
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
        setFiltersActive(false);

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

  const openFilterDialog = () => {
    setDraftMinGrade(filtersActive ? appliedMinGrade : "VB");
    setDraftMaxGrade(filtersActive ? appliedMaxGrade : "V17");
    setDraftSortMode(filtersActive ? appliedSortMode : "recent");
    setFilterOpen(true);
  };

  const handleApplyFilters = async () => {
    if (!wallSectionID || filterSubmitting) return;
    if (gradeIndex(draftMinGrade) > gradeIndex(draftMaxGrade)) {
      toast.error("Minimum grade cannot be harder than maximum grade.");
      return;
    }

    try {
      setFilterSubmitting(true);
      await loadFilteredProblems(user, draftMinGrade, draftMaxGrade, draftSortMode);
      setAppliedMinGrade(draftMinGrade);
      setAppliedMaxGrade(draftMaxGrade);
      setAppliedSortMode(draftSortMode);
      setFiltersActive(true);
      setFilterOpen(false);
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to apply filters.");
    } finally {
      setFilterSubmitting(false);
    }
  };

  const handleClearFilters = async () => {
    if (!wallSectionID || filterSubmitting) return;
    try {
      setFilterSubmitting(true);
      await loadProblems(user);
      setDraftMinGrade("VB");
      setDraftMaxGrade("V17");
      setDraftSortMode("recent");
      setAppliedMinGrade("VB");
      setAppliedMaxGrade("V17");
      setAppliedSortMode("recent");
      setFiltersActive(false);
      setFilterOpen(false);
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to clear filters.");
    } finally {
      setFilterSubmitting(false);
    }
  };

  const handleConfirmDelete = useCallback(async () => {
    if (!canManageWallProblems || !user || !deleteTarget || !wallSectionID || deleteSubmitting) return;
    try {
      setDeleteSubmitting(true);
      await deleteWallSectionProblem(user, wallSectionID, deleteTarget.problemId);
      await reloadProblems(user);
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
    reloadProblems,
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
      setAddUploadProgress(0);
      const created = await createWallSectionProblem(user, wallSectionID, {
        holdColor,
        info,
        assignedGrade: assignedGradeEnum,
      });
      const createdId = created?.problemId;
      if (newProblemPhoto) {
        if (createdId == null) {
          throw new Error("Problem was created but no id was returned for photo upload.");
        }
        await uploadClimbingProblemImage(user, createdId, newProblemPhoto, {
          onProgress: setAddUploadProgress,
        });
      }
      await reloadProblems(user);
      clearAddProblemForm();
      setAddOpen(false);
      toast.success(newProblemPhoto ? "Problem added with photo." : "Problem added.");
    } catch (err) {
      console.error(err);
      const message = err instanceof Error ? err.message : "Failed to add problem.";
      const imageFlowAlreadyToasted =
        /^(Failed to (upload|request|save)|Signed URL|Please choose)/.test(message);
      if (!imageFlowAlreadyToasted) {
        toast.error(message);
      }
      try {
        await reloadProblems(user);
      } catch {
        /* ignore refresh error */
      }
    } finally {
      setAddSubmitting(false);
      setAddUploadProgress(0);
    }
  };

  const handleAddPhotoSelected = async (event) => {
    const input = event.target;
    const file = input.files?.[0] ?? null;
    if (!file) return;

    if (!isAllowedWallImageFile(file)) {
      toast.error("Please choose a phone photo (JPEG/PNG/WebP, or iPhone HEIC/HEIF).");
      input.value = "";
      return;
    }

    try {
      const prepared = await prepareWallImageFile(file, {
        onHeicConvertStart: () => setConvertingHeic(true),
      });
      setNewProblemPhotoPreview((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return URL.createObjectURL(prepared);
      });
      setNewProblemPhoto(prepared);
    } catch (err) {
      console.error("Prepare problem photo failed:", err);
      toast.error(
        err instanceof Error ? err.message : "Could not read that photo. Try another image.",
      );
      input.value = "";
    } finally {
      setConvertingHeic(false);
    }
  };

  const handleResetWallSection = useCallback(async () => {
    if (!canManageWallProblems || !user || !wallSectionID || resetSubmitting) return;
    try {
      setResetSubmitting(true);
      await resetWallSection(user, wallSectionID);
      await loadProblems(user);
      setFiltersActive(false);
      setResetOpen(false);
      toast.success("Wall section reset.");
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Failed to reset wall section.");
    } finally {
      setResetSubmitting(false);
    }
  }, [canManageWallProblems, user, wallSectionID, resetSubmitting, loadProblems]);

  const filterHint = filtersActive
    ? `${appliedMinGrade}–${appliedMaxGrade} · ${
        appliedSortMode === "hardest"
          ? "Hardest"
          : appliedSortMode === "easiest"
            ? "Easiest"
            : "Most Recent"
      }`
    : null;

  const isInvalidGradeRange = gradeIndex(draftMinGrade) > gradeIndex(draftMaxGrade);
  const wallImageURL = sectionImageURL(section);

  if (!ready) return <PageLoader message="Loading…" />;
  if (loading) return <PageLoader message="Loading wall section…" />;

  const selectClassName =
    "w-24 rounded-md border border-input bg-background px-2 py-1.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40";

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

        <section>
          <Card
            className="relative mb-7 gap-0 overflow-hidden py-0 ring-0"
            style={{
              ...card.surface,
              position: "relative",
              overflow: "hidden",
              fontFamily,
              padding: "0",
            }}
          >
            <div style={card.accentBar} aria-hidden />
            <div className="flex flex-col items-stretch sm:flex-row">
              <div className="relative h-48 w-full shrink-0 overflow-hidden bg-muted sm:w-64">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={wallImageURL || WALL_SECTION_PLACEHOLDER_SRC}
                  alt={
                    wallImageURL
                      ? `${section?.wallSectionName || "Wall section"} photo`
                      : "Default wall section photo"
                  }
                  className="absolute inset-0 h-full w-full object-cover"
                />
              </div>
              <CardHeader className="min-w-0 flex-1 rounded-none px-5 pt-5 pb-5">
                <CardTitle className="m-0 text-[1.75rem] font-bold" style={{ color: colors.text }}>
                  {section?.wallSectionName || `Section ${wallSectionID}`}
                </CardTitle>
                {isAdmin && user && section && wallSectionID && (
                  <CardAction>
                    <WallSectionAdminMenu
                      user={user}
                      section={section}
                      onSectionUpdated={handleSectionUpdated}
                      ariaLabel="Wall section actions"
                    />
                  </CardAction>
                )}
                <CardDescription
                  className="mt-2 max-w-[65ch] text-[0.9375rem] leading-[1.55]"
                  style={{ color: colors.muted }}
                >
                  {section?.wallSectionInfo || "No section description available."}
                </CardDescription>
              </CardHeader>
            </div>
          </Card>
        </section>

        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex min-w-0 flex-wrap items-baseline gap-2">
            <h2 className="m-0 text-lg font-semibold" style={{ color: colors.muted }}>
              Problems
            </h2>
            {filterHint && (
              <span className="text-sm" style={{ color: colors.subtle }}>
                {filterHint}
              </span>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              className="shrink-0"
              onClick={openFilterDialog}
            >
              <SlidersHorizontal className="size-4" />
              Filter
            </Button>
            {canManageWallProblems && (
              <>
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
              </>
            )}
          </div>
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

        {problems.length === 0 ? (
          <p className="m-0" style={{ color: colors.subtle }}>
            No problems found for this wall section.
          </p>
        ) : (
          <div className="grid gap-5 [grid-template-columns:repeat(auto-fill,minmax(260px,1fr))]">
            {problems.map((problem) => (
              <article key={problem.problemId}>
                <Card
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
                      src={problemImageURL(problem) || PROBLEM_PLACEHOLDER_SRC}
                      alt={
                        problemImageURL(problem)
                          ? ""
                          : "Default problem photo"
                      }
                      className="h-full w-full object-cover"
                    />
                  </div>
                  <CardHeader className="px-5 pt-5 pb-0">
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle
                        className="min-w-0 text-lg font-semibold leading-[1.35]"
                        style={{ color: colors.text }}
                      >
                        {problem.holdColor}
                      </CardTitle>
                      {canManageWallProblems && user && wallSectionID && (
                        <CardAction>
                          <ClimbingProblemSetterMenu
                            user={user}
                            wallSectionId={wallSectionID}
                            problem={problem}
                            onProblemUpdated={(patch) =>
                              handleProblemUpdated(problem.problemId, patch)
                            }
                            onDeleteProblem={() => setDeleteTarget(problem)}
                          />
                        </CardAction>
                      )}
                    </div>
                  </CardHeader>

                  <CardContent className="flex flex-grow flex-col px-5 pb-0 pt-0">
                    <p className="m-0 text-sm leading-6" style={{ color: colors.muted }}>
                      {problem.info || "No problem notes available."}
                    </p>
                  </CardContent>

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

      <Dialog
        open={filterOpen}
        onOpenChange={(open) => {
          setFilterOpen(open);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Filter Problems</DialogTitle>
            <DialogDescription>
              Choose an inclusive grade range and sort by most recent, easiest, or hardest. Clear
              restores the default list order.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div className="grid gap-1.5">
              <span className="text-sm font-medium text-foreground">Grade range</span>
              <div className="flex items-center gap-2">
                <div className="grid gap-1">
                  <label htmlFor="filter-min-grade" className="sr-only">
                    Minimum grade
                  </label>
                  <select
                    id="filter-min-grade"
                    value={draftMinGrade}
                    onChange={(ev) => setDraftMinGrade(ev.target.value)}
                    className={selectClassName}
                    aria-label="Minimum grade"
                  >
                    {GRADE_OPTIONS.map((g) => (
                      <option key={g} value={g}>
                        {g}
                      </option>
                    ))}
                  </select>
                </div>
                <span className="text-sm font-medium text-muted-foreground" aria-hidden>
                  –
                </span>
                <div className="grid gap-1">
                  <label htmlFor="filter-max-grade" className="sr-only">
                    Maximum grade
                  </label>
                  <select
                    id="filter-max-grade"
                    value={draftMaxGrade}
                    onChange={(ev) => setDraftMaxGrade(ev.target.value)}
                    className={selectClassName}
                    aria-label="Maximum grade"
                  >
                    {GRADE_OPTIONS.map((g) => (
                      <option key={g} value={g}>
                        {g}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
            <fieldset className="grid gap-2">
              <legend className="text-sm font-medium text-foreground">Sort</legend>
              <label className="flex items-center gap-2 text-sm text-foreground">
                <input
                  type="radio"
                  name="filter-sort"
                  value="recent"
                  checked={draftSortMode === "recent"}
                  onChange={() => setDraftSortMode("recent")}
                />
                Most Recent
              </label>
              <label className="flex items-center gap-2 text-sm text-foreground">
                <input
                  type="radio"
                  name="filter-sort"
                  value="easiest"
                  checked={draftSortMode === "easiest"}
                  onChange={() => setDraftSortMode("easiest")}
                />
                Easiest
              </label>
              <label className="flex items-center gap-2 text-sm text-foreground">
                <input
                  type="radio"
                  name="filter-sort"
                  value="hardest"
                  checked={draftSortMode === "hardest"}
                  onChange={() => setDraftSortMode("hardest")}
                />
                Hardest
              </label>
            </fieldset>
            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={filterSubmitting}
                onClick={handleClearFilters}
              >
                Clear filters
              </Button>
              <Button type="button" variant="outline" onClick={() => setFilterOpen(false)}>
                Cancel
              </Button>
              <Button
                type="button"
                disabled={filterSubmitting || isInvalidGradeRange}
                style={buttons.primary}
                className={isInvalidGradeRange ? "opacity-50" : undefined}
                onClick={handleApplyFilters}
              >
                {filterSubmitting ? "Applying…" : "Apply"}
              </Button>
            </DialogFooter>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog
        open={addOpen}
        onOpenChange={(open) => {
          if (addSubmitting) return;
          setAddOpen(open);
          if (!open) clearAddProblemForm();
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Problem</DialogTitle>
            <DialogDescription>
              Enter problem details for this wall section. You can optionally add a photo.
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
              <select
                id="add-problem-grade"
                name="assignedGrade"
                required
                value={newAssignedGrade}
                onChange={(ev) => setNewAssignedGrade(ev.target.value)}
                className={selectClassName}
              >
                {GRADE_OPTIONS.map((g) => (
                  <option key={g} value={g}>
                    {g}
                  </option>
                ))}
              </select>
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
                disabled={addSubmitting}
                value={newProblemInfo}
                onChange={(ev) => setNewProblemInfo(ev.target.value)}
                className="resize-y rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-60"
                placeholder="Short summary for climbers"
              />
            </div>
            <div className="grid gap-2">
              <span className="text-sm font-medium text-foreground">Photo (optional)</span>
              <div className="overflow-hidden rounded-md border border-border">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={newProblemPhotoPreview || PROBLEM_PLACEHOLDER_SRC}
                  alt={
                    newProblemPhotoPreview
                      ? "Selected problem photo"
                      : "Default problem photo"
                  }
                  className="aspect-[16/10] w-full object-cover"
                />
              </div>
              {!newProblemPhoto && !convertingHeic && (
                <p className="m-0 text-sm text-muted-foreground">
                  This default photo appears on the problem until you upload one.
                </p>
              )}
              {convertingHeic && (
                <p className="m-0 text-sm text-muted-foreground" role="status">
                  Uploading iPhone photo…
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
                  disabled={addSubmitting || convertingHeic}
                  onClick={() => addPhotoInputRef.current?.click()}
                >
                  {convertingHeic ? "Uploading…" : newProblemPhoto ? "Change photo" : "Upload photo"}
                </Button>
                {newProblemPhoto && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={addSubmitting || convertingHeic}
                    onClick={clearAddPhoto}
                  >
                    Remove photo
                  </Button>
                )}
              </div>
              {addSubmitting && newProblemPhoto && addUploadProgress > 0 && (
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
              <Button type="submit" disabled={addSubmitting || convertingHeic} style={buttons.primary}>
                {addSubmitting
                  ? newProblemPhoto && addUploadProgress > 0
                    ? `Uploading… ${addUploadProgress}%`
                    : "Adding…"
                  : "Add problem"}
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
