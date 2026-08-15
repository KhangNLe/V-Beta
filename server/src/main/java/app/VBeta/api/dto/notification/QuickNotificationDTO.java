package app.VBeta.api.dto.notification;

import java.time.LocalDateTime;

/**
 * Short inbox item returned to the client for unread notifications.
 *
 * @param event event type name and description
 * @param createdAt notification created-at timestamp
 */
public record QuickNotificationDTO(
    EventTypeDTO event,
    LocalDateTime createdAt
) {}
