package app.VBeta.api.dto.notification;

public record NotificationClickDTO(
    NotificationClickKind kind,
    Long reportId,
    Long wallSectionId,
    Long problemId,
    Long discussionId,
    Long userId
) {
}
