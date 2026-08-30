"use client";

import {
  ADMIN_REASON_MAX_LENGTH,
  APPEAL_RESOLVE_STATUSES,
  fetchAppeals,
  resolveAppeal,
} from "@/api/appeals";
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
  appealHasReportId,
  getAppealContentSummary,
  getAppealFlags,
  getAppealId,
  getAppealQueue,
  getAppealReason,
  getAppealReportId,
  getAppealUserName,
} from "@/lib/appealQueue";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { toast } from "react-toastify";

function AppealQueueView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, account, ready } = useRequireAuth({
    requireEmailVerified: true,
  });
  const [appeals, setAppeals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedAppeal, setSelectedAppeal] = useState(null);
  const [adminComments, setAdminComments] = useState("");
  const [resolving, setResolving] = useState(false);

  const isAdmin = getAccountRole(account).toUpperCase().includes("ADMIN");
  const deepLinkReportId = searchParams.get("reportId");

  const loadQueue = async () => {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const payload = await fetchAppeals(user);
      setAppeals(getAppealQueue(payload));
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      const message =
        err instanceof Error ? err.message : "Failed to load appeals";
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

    const match = appeals.find((appeal) =>
      appealHasReportId(appeal, deepLinkReportId),
    );
    if (match) {
      setSelectedAppeal(match);
      return undefined;
    }

    let cancelled = false;
    (async () => {
      try {
        const payload = await fetchAppeals(user, {
          reportId: Number(deepLinkReportId),
        });
        if (cancelled) return;
        const next = getAppealQueue(payload)[0] ?? null;
        if (!next) {
          toast.error("That appeal is not in the queue.");
          router.replace("/appeal-queue");
          return;
        }
        setSelectedAppeal(next);
      } catch (err) {
        if (cancelled) return;
        toast.error(err instanceof Error ? err.message : "Appeal not found");
        router.replace("/appeal-queue");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, user, isAdmin, loading, appeals, deepLinkReportId, router]);

  const openAppeal = (appeal) => {
    const reportId = getAppealReportId(appeal);
    setSelectedAppeal(appeal);
    setAdminComments("");
    if (reportId) {
      router.replace(`/appeal-queue?reportId=${reportId}`);
    }
  };

  const closeAppeal = () => {
    setSelectedAppeal(null);
    setAdminComments("");
    router.replace("/appeal-queue");
  };

  const comments = adminComments.trim();
  const commentsOverLimit = adminComments.length > ADMIN_REASON_MAX_LENGTH;
  const canResolve =
    comments.length > 0 &&
    !commentsOverLimit &&
    !resolving &&
    getAppealId(selectedAppeal) != null;

  const handleResolve = async (appealStatus) => {
    if (!user || !canResolve) return;
    const appealId = getAppealId(selectedAppeal);
    setResolving(true);
    try {
      await resolveAppeal(user, {
        appealId,
        appealStatus,
        adminReason: comments,
      });
      toast.success(
        appealStatus === APPEAL_RESOLVE_STATUSES.APPROVED
          ? "Appeal approved. Content restored."
          : "Appeal denied.",
      );
      closeAppeal();
      await loadQueue();
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      toast.error(
        err instanceof Error ? err.message : "Failed to resolve appeal",
      );
    } finally {
      setResolving(false);
    }
  };

  if (!ready || loading) {
    return <PageLoader message="Loading appeals…" />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Appeals</CardTitle>
            <CardDescription>Error loading appeals</CardDescription>
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
        <h1 className="mb-1 text-3xl font-bold">Appeals</h1>
        <p className="text-sm text-muted-foreground">
          Open restore requests from content owners. Newest first.
        </p>
      </div>

      {appeals.length === 0 ? (
        <p className="text-sm text-muted-foreground">No open appeals.</p>
      ) : (
        <ul className="grid gap-2">
          {appeals.map((appeal) => {
            const appealId = getAppealId(appeal);
            return (
              <li key={appealId ?? getAppealReportId(appeal)}>
                <button
                  type="button"
                  className="w-full rounded-md border border-border px-4 py-3 text-left hover:bg-muted"
                  onClick={() => openAppeal(appeal)}
                >
                  <p className="text-sm font-medium">
                    {getAppealUserName(appeal) || "Unknown owner"}
                  </p>
                  <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
                    {getAppealReason(appeal) || "No appeal reason"}
                  </p>
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

      <AppealDetailDialog
        appeal={selectedAppeal}
        adminComments={adminComments}
        commentsOverLimit={commentsOverLimit}
        canResolve={canResolve}
        resolving={resolving}
        onCommentsChange={setAdminComments}
        onClose={closeAppeal}
        onApprove={() => handleResolve(APPEAL_RESOLVE_STATUSES.APPROVED)}
        onDeny={() => handleResolve(APPEAL_RESOLVE_STATUSES.DENIED)}
      />
    </div>
  );
}

function AppealDetailDialog({
  appeal,
  adminComments,
  commentsOverLimit,
  canResolve,
  resolving,
  onCommentsChange,
  onClose,
  onApprove,
  onDeny,
}) {
  const flags = getAppealFlags(appeal);
  return (
    <Dialog
      open={appeal != null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>User appeal</DialogTitle>
          <DialogDescription>
            Review the owner’s restore request, then approve or deny with
            comments.
          </DialogDescription>
        </DialogHeader>
        {appeal ? (
          <div className="grid max-h-[60vh] gap-3 overflow-y-auto py-1">
            <section className="grid gap-1">
              <h2 className="text-sm font-medium">Appellant</h2>
              <p className="text-sm text-muted-foreground">
                {getAppealUserName(appeal) || "Not available"}
              </p>
            </section>
            <section className="grid gap-1">
              <h2 className="text-sm font-medium">Appeal reason</h2>
              <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                {getAppealReason(appeal) || "Not available"}
              </p>
            </section>
            <section className="grid gap-1">
              <h2 className="text-sm font-medium">Removed content</h2>
              <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                {getAppealContentSummary(appeal) || "Not available"}
              </p>
            </section>
            {flags.map((flag, index) => (
              <section key={`${flag.reporter}-${index}`} className="grid gap-1">
                <h2 className="text-sm font-medium">
                  Report{flags.length > 1 ? ` ${index + 1}` : ""}
                </h2>
                <p className="text-sm text-muted-foreground">
                  {flag.reporter || "Unknown reporter"} · {flag.category}
                </p>
                <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                  {flag.reason || "Not available"}
                </p>
              </section>
            ))}
            <div className="grid gap-1.5">
              <label htmlFor="admin-comments" className="text-sm font-medium">
                Admin comments
              </label>
              <textarea
                id="admin-comments"
                value={adminComments}
                onChange={(event) => onCommentsChange(event.target.value)}
                disabled={resolving}
                required
                rows={4}
                placeholder="Required. Explain the approve or deny decision."
                aria-label="Admin comments"
                className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
              />
              <p className="text-right text-xs text-muted-foreground">
                {adminComments.length}/{ADMIN_REASON_MAX_LENGTH}
              </p>
              {commentsOverLimit ? (
                <p className="text-xs text-red-500">
                  Comments must be at most {ADMIN_REASON_MAX_LENGTH} characters.
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
            variant="destructive"
            disabled={!canResolve}
            onClick={onDeny}
          >
            {resolving ? "Saving…" : "Deny"}
          </Button>
          <Button type="button" disabled={!canResolve} onClick={onApprove}>
            {resolving ? "Saving…" : "Approve"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function AppealQueuePage() {
  return (
    <Suspense fallback={<PageLoader message="Loading appeals…" />}>
      <AppealQueueView />
    </Suspense>
  );
}
