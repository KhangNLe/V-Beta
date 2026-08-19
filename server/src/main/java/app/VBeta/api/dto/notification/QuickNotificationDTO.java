package app.VBeta.api.dto.notification;

import java.time.Instant;

/**
 * Short inbox item returned to the client for unread notifications.
 * <p>
 * {@code summary.eventTypeName} is display copy. {@code click} is the redirect
 * target computed at read time from the event's typed FK (not a stored href).
 * Report reason and admin notes are omitted.
 *
 * @param notificationId inbox row id (used to mark read)
 * @param summary catalog event type name and description
 * @param click redirect kind and target ids
 * @param createdAt notification created-at timestamp
 */
public record QuickNotificationDTO(
        Long notificationId,
        EventTypeDTO summary,
        NotificationClickDTO click,
        Instant createdAt
) {}
