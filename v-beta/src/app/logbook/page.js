"use client";

import {
  fetchAllLogbookEntries,
  fetchLogbook,
  LOGBOOK_PAGE_SIZE,
} from "@/api/moderation";
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
  buildLogbookTxt,
  downloadTextFile,
  formatLogbookDecision,
  getLogbookActorName,
  getLogbookId,
  getLogbookNotes,
  getLogbookProblemHref,
  getLogbookReport,
  getLogbookReportHref,
  getLogbookReportId,
} from "@/lib/moderationLogbook";
import { formatReportTime } from "@/lib/reportQueue";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { toast } from "react-toastify";

function LogbookView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, account, ready } = useRequireAuth({
    requireEmailVerified: true,
  });
  const [entries, setEntries] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [downloading, setDownloading] = useState(false);

  const isAdmin = getAccountRole(account).toUpperCase().includes("ADMIN");
  const deepLinkModerationId = searchParams.get("moderationId");

  const loadPage = async (nextPage) => {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const payload = await fetchLogbook(user, { offSetPlace: nextPage });
      setEntries(payload.moderationLogs);
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      const message =
        err instanceof Error ? err.message : "Failed to load logbook";
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
    loadPage(page);
  }, [ready, user, isAdmin, router, page]);

  useEffect(() => {
    if (!ready || !user || !isAdmin || loading) return undefined;
    if (!deepLinkModerationId) return undefined;

    const parsed = Number(deepLinkModerationId);
    const match = entries.find((entry) => getLogbookId(entry) === parsed);
    if (match) {
      setSelected(match);
      return undefined;
    }

    let cancelled = false;
    (async () => {
      try {
        const payload = await fetchLogbook(user, { moderationId: parsed });
        if (cancelled) return;
        const next = payload.moderationLogs[0] ?? null;
        if (!next) {
          toast.error("That logbook row was not found.");
          router.replace("/logbook");
          return;
        }
        setSelected(next);
      } catch (err) {
        if (cancelled) return;
        toast.error(
          err instanceof Error ? err.message : "Moderation not found",
        );
        router.replace("/logbook");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, user, isAdmin, loading, entries, deepLinkModerationId, router]);

  const openEntry = (entry) => {
    const moderationId = getLogbookId(entry);
    setSelected(entry);
    if (moderationId) {
      router.replace(`/logbook?moderationId=${moderationId}`);
    }
  };

  const closeEntry = () => {
    setSelected(null);
    router.replace("/logbook");
  };

  const handleDownload = async () => {
    if (!user || downloading) return;
    setDownloading(true);
    try {
      const allEntries = await fetchAllLogbookEntries(user);
      const stamp = new Date().toISOString().slice(0, 10);
      downloadTextFile(
        `v-beta-moderation-logbook-${stamp}.txt`,
        buildLogbookTxt(allEntries),
      );
      toast.success("Logbook downloaded.");
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      toast.error(
        err instanceof Error ? err.message : "Failed to download logbook",
      );
    } finally {
      setDownloading(false);
    }
  };

  if (!ready || loading) {
    return <PageLoader message="Loading logbook…" />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Logbook</CardTitle>
            <CardDescription>Error loading logbook</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-red-500">{error}</p>
            <Button onClick={() => loadPage(page)} className="mt-4">
              Try Again
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const hasPrevious = page > 1;
  const hasNext = entries.length >= LOGBOOK_PAGE_SIZE;

  return (
    <div className="container mx-auto min-h-screen p-4">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold mb-1">Logbook</h1>
          <p className="text-sm text-muted-foreground">
            Append-only moderation decisions. Newest first. This log cannot be
            edited.
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          onClick={handleDownload}
          disabled={downloading}
        >
          {downloading ? "Preparing…" : "Download .txt"}
        </Button>
      </div>

      {entries.length === 0 ? (
        <p className="text-sm text-muted-foreground">No logbook entries.</p>
      ) : (
        <ul className="grid gap-3" aria-label="Moderation logbook">
          {entries.map((entry) => {
            const moderationId = getLogbookId(entry);
            const reportId = getLogbookReportId(entry);
            const decision =
              typeof entry.decision === "string" ? entry.decision : "";
            return (
              <li key={moderationId || `${decision}-${entry.createdAt}`}>
                <button
                  type="button"
                  className="flex w-full flex-col gap-1 rounded-lg border border-border bg-card px-4 py-3 text-left hover:bg-muted/50"
                  onClick={() => openEntry(entry)}
                >
                  <span className="flex flex-wrap items-baseline justify-between gap-2">
                    <span className="text-sm font-semibold">
                      {formatLogbookDecision(decision)}
                    </span>
                    <time className="text-xs text-muted-foreground">
                      {formatReportTime(
                        typeof entry.createdAt === "string" ||
                          typeof entry.createdAt === "number"
                          ? String(entry.createdAt)
                          : null,
                      )}
                    </time>
                  </span>
                  <span className="text-sm text-muted-foreground">
                    {getLogbookActorName(entry)}
                    {reportId ? ` · report ${reportId}` : ""}
                  </span>
                  {getLogbookNotes(entry) ? (
                    <span className="line-clamp-2 text-sm">
                      {getLogbookNotes(entry)}
                    </span>
                  ) : null}
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {hasPrevious || hasNext ? (
        <div className="mt-4 flex items-center justify-between gap-2">
          <Button
            type="button"
            variant="outline"
            disabled={!hasPrevious}
            onClick={() => setPage((current) => Math.max(1, current - 1))}
          >
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">Page {page}</span>
          <Button
            type="button"
            variant="outline"
            disabled={!hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </Button>
        </div>
      ) : null}

      <LogbookDetailDialog entry={selected} onClose={closeEntry} />
    </div>
  );
}

function LogbookDetailDialog({ entry, onClose }) {
  const report = getLogbookReport(entry);
  const reportHref = getLogbookReportHref(entry);
  const problemHref = getLogbookProblemHref(entry);
  const reportId = getLogbookReportId(entry);
  const decision =
    entry && typeof entry.decision === "string" ? entry.decision : "";
  const discussion =
    report?.discussion && typeof report.discussion === "object"
      ? report.discussion
      : null;
  const content =
    typeof discussion?.discussionContent === "string"
      ? discussion.discussionContent
      : "";

  return (
    <Dialog
      open={entry != null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Logbook entry</DialogTitle>
          <DialogDescription>
            Read-only record of a moderation decision.
          </DialogDescription>
        </DialogHeader>

        {entry ? (
          <div className="grid max-h-[60vh] gap-3 overflow-y-auto py-1">
            <DetailRow label="Decision">
              {formatLogbookDecision(decision)}
            </DetailRow>
            <DetailRow label="Admin">{getLogbookActorName(entry)}</DetailRow>
            <DetailRow label="Time">
              {formatReportTime(
                typeof entry.createdAt === "string" ||
                  typeof entry.createdAt === "number"
                  ? String(entry.createdAt)
                  : null,
              )}
            </DetailRow>
            <DetailRow label="Report">
              {reportHref && reportId ? (
                <Link href={reportHref} className="underline">
                  Report {reportId}
                </Link>
              ) : reportId ? (
                `Report ${reportId}`
              ) : (
                "Not available"
              )}
            </DetailRow>
            {problemHref ? (
              <DetailRow label="Problem page">
                <Link href={problemHref} className="underline">
                  Open discussion
                </Link>
              </DetailRow>
            ) : null}
            <DetailRow label="Admin notes">
              <p className="whitespace-pre-wrap">
                {getLogbookNotes(entry) || "—"}
              </p>
            </DetailRow>
            {content ? (
              <DetailRow label="Reported content">
                <p className="whitespace-pre-wrap">{content}</p>
              </DetailRow>
            ) : null}
          </div>
        ) : null}

        <DialogFooter className="mt-1">
          <Button type="button" variant="outline" onClick={onClose}>
            Close
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

export default function LogbookPage() {
  return (
    <Suspense fallback={<PageLoader message="Loading logbook…" />}>
      <LogbookView />
    </Suspense>
  );
}
