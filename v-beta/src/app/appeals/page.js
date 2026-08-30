"use client";

import {
  APPEAL_REASON_MAX_LENGTH,
  createAppeal,
  fetchDeletionNotice,
} from "@/api/appeals";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import {
  canSubmitAppeal,
  getAppealStatusLabel,
  getDeletionAdminReason,
  getOwnerAppealReason,
  getOwnerReportCategorySummary,
  getOwnerReportReasonSummary,
  getRemovedContentSummary,
} from "@/lib/ownerAppeal";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { toast } from "react-toastify";

function OwnerAppealView() {
  const searchParams = useSearchParams();
  const { user, ready } = useRequireAuth({
    redirectMode: "push",
    requireEmailVerified: true,
  });
  const reportId = Number(searchParams.get("reportId"));
  const hasReportId = Number.isInteger(reportId) && reportId > 0;

  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [appealReason, setAppealReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const loadNotice = async () => {
    if (!user || !hasReportId) return;
    try {
      setLoading(true);
      setError(null);
      const payload = await fetchDeletionNotice(user, reportId);
      setNotice(payload);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Appeal is not allowed";
      setNotice(null);
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!ready) return;
    if (!user) return;
    if (!hasReportId) {
      setLoading(false);
      setError(null);
      setNotice(null);
      return;
    }
    loadNotice();
  }, [ready, user, hasReportId, reportId]);

  const canAppeal = canSubmitAppeal(notice) && !submitted;
  const reason = appealReason.trim();
  const reasonOverLimit = appealReason.length > APPEAL_REASON_MAX_LENGTH;
  const canSend =
    canAppeal && reason.length > 0 && !reasonOverLimit && !submitting;

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!user || !canSend) return;
    setSubmitting(true);
    try {
      await createAppeal(user, { reportId, appealReason: reason });
      setSubmitted(true);
      setNotice((current) =>
        current
          ? {
              ...current,
              canAppeal: false,
              appealStatus: "OPEN",
              reportStatus: "APPEAL_PENDING",
              appeal: {
                ...(current.appeal && typeof current.appeal === "object"
                  ? current.appeal
                  : {}),
                appealReason: reason,
              },
            }
          : current,
      );
      toast.success("Appeal submitted.");
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Failed to submit appeal";
      if (message.includes("Appeal already exists")) {
        setSubmitted(true);
        setNotice((current) =>
          current
            ? { ...current, canAppeal: false, appealStatus: "OPEN" }
            : current,
        );
        toast.error("An appeal was already submitted for this removal.");
      } else {
        toast.error(message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (!ready || (hasReportId && loading)) {
    return <PageLoader message="Loading appeal…" />;
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <Card className="border border-border">
        <CardHeader>
          <CardTitle>Content removed</CardTitle>
          <CardDescription>
            {!hasReportId
              ? "Open this page from a content-removed notification."
              : canAppeal
                ? `Report ${reportId}. You can appeal this removal once.`
                : `Report ${reportId}.`}
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          {!hasReportId ? (
            <p className="text-sm text-muted-foreground">
              No report was selected. Use the notification inbox to open your
              deletion notice.
            </p>
          ) : error && !notice ? (
            <p className="text-sm text-red-500">{error}</p>
          ) : (
            <>
              <section className="grid gap-1">
                <h2 className="text-sm font-medium">Admin reason</h2>
                <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                  {getDeletionAdminReason(notice) || "Not available"}
                </p>
              </section>
              <section className="grid gap-1">
                <h2 className="text-sm font-medium">Removed content</h2>
                <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                  {getRemovedContentSummary(notice) || "Not available"}
                </p>
              </section>
              <section className="grid gap-1">
                <h2 className="text-sm font-medium">Report category</h2>
                <p className="text-sm text-muted-foreground">
                  {getOwnerReportCategorySummary(notice) || "Not available"}
                </p>
              </section>
              <section className="grid gap-1">
                <h2 className="text-sm font-medium">Report reason</h2>
                <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                  {getOwnerReportReasonSummary(notice) || "Not available"}
                </p>
              </section>
              {getOwnerAppealReason(notice) ? (
                <section className="grid gap-1">
                  <h2 className="text-sm font-medium">Your appeal</h2>
                  <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                    {getOwnerAppealReason(notice)}
                  </p>
                </section>
              ) : null}
              <p className="text-sm font-medium">{getAppealStatusLabel(notice)}</p>
              {canAppeal ? (
                <form className="grid gap-2" onSubmit={handleSubmit}>
                  <label htmlFor="appeal-reason" className="text-sm font-medium">
                    Appeal reason
                  </label>
                  <textarea
                    id="appeal-reason"
                    value={appealReason}
                    onChange={(event) => setAppealReason(event.target.value)}
                    disabled={submitting}
                    required
                    rows={4}
                    placeholder="Explain why this content should be restored."
                    aria-label="Appeal reason"
                    className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                  />
                  <p className="text-right text-xs text-muted-foreground">
                    {appealReason.length}/{APPEAL_REASON_MAX_LENGTH}
                  </p>
                  <Button type="submit" disabled={!canSend}>
                    {submitting ? "Submitting…" : "Submit appeal"}
                  </Button>
                </form>
              ) : (
                <p className="text-sm text-muted-foreground">
                  The appeal form is closed for this report.
                </p>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default function AppealsPage() {
  return (
    <Suspense fallback={<PageLoader message="Loading appeal…" />}>
      <OwnerAppealView />
    </Suspense>
  );
}
