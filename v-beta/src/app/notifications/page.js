"use client";

import {
  fetchAllNotifications,
  markNotificationRead,
} from "@/api/notifications";
import PageLoader from "@/components/ui/PageLoader";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import {
  formatNotificationTime,
  getNotificationCreatedAt,
  getNotificationDescription,
  getNotificationHref,
  getNotificationId,
  getNotificationTypeLabel,
  isNotificationUnread,
} from "@/lib/notificationNavigation";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

export default function NotificationsPage() {
  const router = useRouter();
  const { user, ready } = useRequireAuth({
    redirectMode: "push",
    requireEmailVerified: true,
  });
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openingId, setOpeningId] = useState(null);

  useEffect(() => {
    if (!ready || !user) return undefined;
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setError(null);
        const inbox = await fetchAllNotifications(user);
        if (!cancelled) setItems(inbox);
      } catch (err) {
        if (cancelled) return;
        const message =
          err instanceof Error ? err.message : "Failed to load notifications";
        setError(message);
        toast.error("Failed to load notifications");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, user]);

  const handleOpenNotification = async (item) => {
    const notificationId = getNotificationId(item);
    if (!user || !notificationId || openingId) return;

    setOpeningId(notificationId);
    if (isNotificationUnread(item)) {
      try {
        await markNotificationRead(user, notificationId);
        setItems((current) =>
          current.map((row) =>
            getNotificationId(row) === notificationId
              ? { ...row, readAt: new Date().toISOString() }
              : row,
          ),
        );
      } catch (err) {
        toast.error(
          `Failed to mark notification read: ${
            err instanceof Error ? err.message : "Unknown error."
          }`,
        );
      }
    }
    setOpeningId(null);
    router.push(getNotificationHref(item));
  };

  if (!ready || loading) {
    return <PageLoader message="Loading notifications…" />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Notifications</CardTitle>
            <CardDescription>Error loading notifications</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-red-500">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <Card className="border border-border">
        <CardHeader>
          <CardTitle>All notifications</CardTitle>
          <CardDescription>
            Read and unread inbox items. Opening one takes you to the related
            report or appeal.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {items.length === 0 ? (
            <p className="text-sm text-muted-foreground">No notifications.</p>
          ) : (
            <ul className="grid gap-2" aria-label="All notifications">
              {items.map((item) => {
                const notificationId = getNotificationId(item);
                const unread = isNotificationUnread(item);
                const eventType = getNotificationTypeLabel(item);
                const description = getNotificationDescription(item);
                const createdAt = formatNotificationTime(
                  getNotificationCreatedAt(item),
                );
                return (
                  <li key={notificationId || eventType + createdAt}>
                    <button
                      type="button"
                      className={`flex w-full items-start gap-3 rounded-lg border border-border px-3 py-3 text-left hover:bg-muted/50 disabled:opacity-60 ${
                        unread ? "bg-background" : "bg-muted/30"
                      }`}
                      disabled={openingId != null}
                      onClick={() => handleOpenNotification(item)}
                    >
                      <span
                        className={`mt-1.5 size-2 shrink-0 rounded-full ${
                          unread ? "bg-primary" : "bg-muted-foreground/40"
                        }`}
                        aria-hidden="true"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="flex flex-wrap items-baseline justify-between gap-2">
                          <span
                            className={`text-sm ${
                              unread
                                ? "font-semibold text-foreground"
                                : "font-medium text-muted-foreground"
                            }`}
                          >
                            {eventType}
                          </span>
                          <time className="text-xs text-muted-foreground">
                            {createdAt}
                          </time>
                        </span>
                        <span className="mt-1 block text-sm text-muted-foreground">
                          {description || "Open this notification."}
                        </span>
                        <span className="sr-only">
                          {unread ? "Unread" : "Read"}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
