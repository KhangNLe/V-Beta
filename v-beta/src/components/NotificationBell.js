"use client";

import { usePathname, useRouter } from "next/navigation";
import { Bell } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import {
  fetchUnreadNotifications,
  markNotificationRead,
} from "@/api/notifications";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  formatNotificationTime,
  getNotificationCreatedAt,
  getNotificationDescription,
  getNotificationHref,
  getNotificationId,
  getNotificationTypeLabel,
} from "@/lib/notificationNavigation";

/**
 * Header bell dropdown of unread notifications.
 *
 * @param {{ user: import("firebase/auth").User }} props
 */
export default function NotificationBell({ user }) {
  const pathname = usePathname();
  const router = useRouter();
  const [unreadItems, setUnreadItems] = useState([]);
  const [openingId, setOpeningId] = useState(null);

  const loadUnread = useCallback(async () => {
    if (!user) {
      setUnreadItems([]);
      return;
    }
    try {
      const items = await fetchUnreadNotifications(user);
      setUnreadItems(items);
    } catch {
      setUnreadItems([]);
    }
  }, [user]);

  useEffect(() => {
    loadUnread();
  }, [loadUnread, pathname]);

  const unreadCount = unreadItems.length;

  const handleOpenNotification = async (item) => {
    const notificationId = getNotificationId(item);
    if (!user || !notificationId || openingId) return;

    setOpeningId(notificationId);
    try {
      await markNotificationRead(user, notificationId);
      setUnreadItems((current) =>
        current.filter((row) => getNotificationId(row) !== notificationId),
      );
    } catch {
      // still navigate to the notification target
    } finally {
      setOpeningId(null);
    }

    router.push(getNotificationHref(item));
  };

  return (
    <DropdownMenu
      onOpenChange={(open) => {
        if (open) loadUnread();
      }}
    >
      <DropdownMenuTrigger
        render={
          <button
            type="button"
            className={`role-navbar__bell ${pathname === "/notifications" ? "role-navbar__bell--active" : ""}`}
            aria-label={
              unreadCount > 0
                ? `Notifications, ${unreadCount} unread`
                : "Notifications"
            }
          />
        }
      >
        <Bell className="role-navbar__bell-icon" aria-hidden="true" />
        {unreadCount > 0 ? (
          <span className="role-navbar__bell-badge">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        ) : null}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80 min-w-80 p-1">
        {unreadItems.length === 0 ? (
          <div className="px-2 py-3 text-sm text-muted-foreground">
            No unread notifications.
          </div>
        ) : (
          unreadItems.map((item) => {
            const notificationId = getNotificationId(item);
            const eventType = getNotificationTypeLabel(item);
            const description = getNotificationDescription(item);
            const createdAt = formatNotificationTime(getNotificationCreatedAt(item));
            return (
              <DropdownMenuItem
                key={notificationId || eventType + createdAt}
                className="items-start gap-2 py-2 whitespace-normal"
                disabled={openingId != null}
                onClick={() => handleOpenNotification(item)}
              >
                <span
                  className="mt-1.5 size-2 shrink-0 rounded-full bg-primary"
                  aria-hidden="true"
                />
                <span className="min-w-0 flex-1">
                  <span className="flex items-baseline justify-between gap-2">
                    <span className="text-sm font-semibold">{eventType}</span>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {createdAt}
                    </span>
                  </span>
                  <span className="mt-0.5 line-clamp-2 block text-xs text-muted-foreground">
                    {description || "Open this notification."}
                  </span>
                </span>
              </DropdownMenuItem>
            );
          })
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          className="justify-center font-medium"
          onClick={() => router.push("/notifications")}
        >
          Show all notifications
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
