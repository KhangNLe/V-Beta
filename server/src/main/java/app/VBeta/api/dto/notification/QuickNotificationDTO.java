package app.VBeta.api.dto.notification;

import java.time.LocalDateTime;

public record QuickNotificationDTO(
    EventTypeDTO event,
    LocalDateTime createdAt
) {}
