"use client";

import {
  ADMIN_NOTES_MAX_LENGTH,
  fetchReportQueue,
  REPORT_RESOLVE_DECISIONS,
  resolveReports,
} from "@/api/reports";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
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
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getAccountRole } from "@/lib/accountSession";
import {
  caseHasReportId,
  formatReportCategory,
  formatReportTime,
  getCaseCategorySummary,
  getCaseCreatedAt,
  getCasePrimaryReportId,
  getCaseQueueScore,
  getCaseReport,
  getCaseReporterSummary,
  getCaseReporters,
  getCaseReportIds,
  getCaseTargetType,
  inferVideoMimeType,
} from "@/lib/reportQueue";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { toast } from "react-toastify";

function ReportsQueue() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, account, ready } = useRequireAuth({
    requireEmailVerified: true,
  });
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedCase, setSelectedCase] = useState(null);
  const [adminNotes, setAdminNotes] = useState("");
  const [resolving, setResolving] = useState(false);

  const isAdmin = getAccountRole(account).toUpperCase().includes("ADMIN");
  const deepLinkReportId = searchParams.get("reportId");

  const loadQueue = async () => {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const payload = await fetchReportQueue(user);
      setCases(payload.reports);
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      const message =
        err instanceof Error ? err.message : "Failed to load reports";
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!ready) return;
    if (!user) return;
    if (!isAdmin) {
      router.replace("/main-page");
      return;
    }
    loadQueue();
  }, [ready, user, isAdmin, router]);

  useEffect(() => {
    if (!ready || !user || !isAdmin || loading) return undefined;
    if (!deepLinkReportId) return undefined;

    const match = cases.find((queueCase) =>
      caseHasReportId(queueCase, deepLinkReportId),
    );
    if (match) {
      setSelectedCase(match);
      return undefined;
    }

    let cancelled = false;
    (async () => {
      try {
        const payload = await fetchReportQueue(user, Number(deepLinkReportId));
        if (cancelled) return;
        const next = payload.reports[0] ?? null;
        if (!next) {
          toast.error("That report is not in the open queue.");
          router.replace("/reports");
          return;
        }
        setSelectedCase(next);
      } catch (err) {
        if (cancelled) return;
        toast.error(
          err instanceof Error ? err.message : "Report not found",
        );
        router.replace("/reports");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, user, isAdmin, loading, cases, deepLinkReportId, router]);

  const openCase = (queueCase) => {
    const reportId = getCasePrimaryReportId(queueCase);
    setSelectedCase(queueCase);
    setAdminNotes("");
    if (reportId) {
      router.replace(`/reports?reportId=${reportId}`);
    }
  };

  const closeCase = () => {
    setSelectedCase(null);
    setAdminNotes("");
    router.replace("/reports");
  };

  const notes = adminNotes.trim();
  const notesOverLimit = adminNotes.length > ADMIN_NOTES_MAX_LENGTH;
  const canResolve =
    notes.length > 0 &&
    !notesOverLimit &&
    !resolving &&
    getCaseTargetType(selectedCase) === "DISCUSSION" &&
    getCaseReportIds(selectedCase).length > 0;

  const handleResolve = async (decision) => {
    if (!user || !canResolve) return;
    setResolving(true);
    try {
      await resolveReports(user, {
        reportIds: getCaseReportIds(selectedCase),
        decision,
        reason: notes,
      });
      toast.success(
        decision === REPORT_RESOLVE_DECISIONS.REMOVE
          ? "Content removed."
          : "Report dismissed.",
      );
      closeCase();
      await loadQueue();
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      toast.error(
        err instanceof Error ? err.message : "Failed to resolve report",
      );
    } finally {
      setResolving(false);
    }
  };

  if (!ready || loading) {
    return <PageLoader message="Loading reports…" />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Reports</CardTitle>
            <CardDescription>Error loading reports</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-red-500">{error}</p>
            <Button onClick={loadQueue} className="mt-4">
              Try Again
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-1">Reports</h1>
        <p className="text-sm text-muted-foreground">
          Ranked open cases. Highest category weight, then more reports, first.
        </p>
      </div>

      {cases.length === 0 ? (
        <p className="text-sm text-muted-foreground">No open reports.</p>
      ) : (
        <ul className="grid gap-3" aria-label="Report queue">
          {cases.map((queueCase) => {
            const primaryId = getCasePrimaryReportId(queueCase);
            const report = getCaseReport(queueCase);
            const discussion =
              report?.discussion && typeof report.discussion === "object"
                ? report.discussion
                : null;
            const kind =
              typeof discussion?.discussionType === "string"
                ? discussion.discussionType
                : getCaseTargetType(queueCase);
            return (
              <li key={primaryId || getCaseCategorySummary(queueCase)}>
                <button
                  type="button"
                  className="flex w-full flex-col gap-1 rounded-lg border border-border bg-card px-4 py-3 text-left hover:bg-muted/50"
                  onClick={() => openCase(queueCase)}
                >
                  <span className="flex flex-wrap items-baseline justify-between gap-2">
                    <span className="text-sm font-semibold">
                      {getCaseCategorySummary(queueCase)}
                    </span>
                    <time className="text-xs text-muted-foreground">
                      {formatReportTime(getCaseCreatedAt(queueCase))}
                    </time>
                  </span>
                  <span className="text-sm text-muted-foreground">
                    {getCaseReporterSummary(queueCase)}
                    {kind ? ` · ${kind}` : ""}
                    {` · score ${getCaseQueueScore(queueCase)}`}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <div className="mt-6">
        <Button onClick={loadQueue} variant="outline">
          Refresh queue
        </Button>
      </div>

      <ReportDetailDialog
        queueCase={selectedCase}
        adminNotes={adminNotes}
        notesOverLimit={notesOverLimit}
        canResolve={canResolve}
        resolving={resolving}
        onNotesChange={setAdminNotes}
        onClose={closeCase}
        onDismiss={() => handleResolve(REPORT_RESOLVE_DECISIONS.DISMISS)}
        onRemove={() => handleResolve(REPORT_RESOLVE_DECISIONS.REMOVE)}
      />
    </div>
  );
}

function ReportDetailDialog({
  queueCase,
  adminNotes,
  notesOverLimit,
  canResolve,
  resolving,
  onNotesChange,
  onClose,
  onDismiss,
  onRemove,
}) {
  const report = getCaseReport(queueCase);
  const discussion =
    report?.discussion && typeof report.discussion === "object"
      ? report.discussion
      : null;
  const wall =
    report?.wallSection && typeof report.wallSection === "object"
      ? report.wallSection
      : null;
  const problem =
    report?.climbingProblem && typeof report.climbingProblem === "object"
      ? report.climbingProblem
      : null;
  const wallId = Number(wall?.wallSectionID);
  const problemId = Number(problem?.problemId);
  const discussionHref =
    Number.isInteger(wallId) &&
    wallId > 0 &&
    Number.isInteger(problemId) &&
    problemId > 0
      ? `/wall/${wallId}/problem/${problemId}`
      : null;
  const isBeta = discussion?.discussionType === "BETA";
  const content =
    typeof discussion?.discussionContent === "string"
      ? discussion.discussionContent
      : "";
  const isDiscussion = getCaseTargetType(queueCase) === "DISCUSSION";

  return (
    <Dialog
      open={queueCase != null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Report detail</DialogTitle>
          <DialogDescription>
            Review reported content, then dismiss or remove it with notes.
          </DialogDescription>
        </DialogHeader>

        {queueCase ? (
          <div className="grid max-h-[60vh] gap-3 overflow-y-auto py-1">
            <DetailRow label="Wall section">
              {discussionHref && wall?.wallSectionName ? (
                <Link href={`/wall/${wallId}`} className="underline">
                  {wall.wallSectionName}
                </Link>
              ) : (
                wall?.wallSectionName || "Not available"
              )}
            </DetailRow>
            <DetailRow label="Problem">
              {discussionHref && problem?.holdColor ? (
                <Link href={discussionHref} className="underline">
                  {problem.holdColor}
                  {problem.assignedGrade ? ` · ${problem.assignedGrade}` : ""}
                </Link>
              ) : (
                problem?.holdColor || "Not available"
              )}
            </DetailRow>
            <DetailRow label="Reported content">
              {isBeta && content ? (
                <div className="grid gap-2">
                  <video
                    controls
                    className="max-h-48 w-full rounded-md bg-black"
                    src={content}
                  >
                    <source src={content} type={inferVideoMimeType(content)} />
                  </video>
                  <a
                    href={content}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs underline"
                  >
                    Open video
                  </a>
                </div>
              ) : content ? (
                <p className="whitespace-pre-wrap">{content}</p>
              ) : (
                "Not available"
              )}
            </DetailRow>
            {discussionHref ? (
              <DetailRow label="Problem page">
                <Link href={discussionHref} className="underline">
                  Open discussion
                </Link>
              </DetailRow>
            ) : null}

            <div className="grid gap-2">
              <p className="text-sm font-medium">Reporters</p>
              <ul className="grid gap-2">
                {getCaseReporters(queueCase).map((row) => {
                  const reporter =
                    row.reporter && typeof row.reporter === "object"
                      ? row.reporter
                      : {};
                  return (
                    <li
                      key={row.reportId}
                      className="rounded-md border border-border px-3 py-2 text-sm"
                    >
                      <p>
                        {reporter.username || "Unknown"} ·{" "}
                        {formatReportCategory(row.categoryName)}
                      </p>
                      <p className="text-muted-foreground">
                        {formatReportTime(
                          typeof row.createdAt === "string" ||
                            typeof row.createdAt === "number"
                            ? String(row.createdAt)
                            : null,
                        )}
                      </p>
                      <p className="mt-1 whitespace-pre-wrap">
                        {typeof row.reportReason === "string"
                          ? row.reportReason
                          : ""}
                      </p>
                    </li>
                  );
                })}
              </ul>
            </div>

            <div className="grid gap-1.5">
              <label htmlFor="admin-notes" className="text-sm font-medium">
                Admin notes
              </label>
              <textarea
                id="admin-notes"
                value={adminNotes}
                onChange={(event) => onNotesChange(event.target.value)}
                disabled={resolving || !isDiscussion}
                required
                rows={4}
                placeholder="Required. Explain the dismiss or deletion decision."
                aria-label="Admin notes"
                className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
              />
              <p className="text-right text-xs text-muted-foreground">
                {adminNotes.length}/{ADMIN_NOTES_MAX_LENGTH}
              </p>
              {!isDiscussion ? (
                <p className="text-xs text-muted-foreground">
                  Queue resolve is discussion comments and betas only.
                </p>
              ) : null}
            </div>
          </div>
        ) : null}

        <DialogFooter className="mt-1 gap-2 sm:justify-end">
          <Button type="button" variant="outline" onClick={onClose}>
            Close
          </Button>
          <Button
            type="button"
            variant="outline"
            disabled={!canResolve}
            onClick={onDismiss}
          >
            {resolving ? "Saving…" : "Dismiss"}
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={!canResolve}
            onClick={onRemove}
          >
            {resolving ? "Saving…" : "Approve deletion"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DetailRow({ label, children }) {
  return (
    <div className="grid gap-1">
      <p className="text-sm font-medium">{label}</p>
      <div className="text-sm text-muted-foreground">{children}</div>
    </div>
  );
}

export default function ReportsPage() {
  return (
    <Suspense fallback={<PageLoader message="Loading reports…" />}>
      <ReportsQueue />
    </Suspense>
  );
}
