package app.VBeta.api.dto.notification;

/**
 * Redirect metadata for one inbox item.
 * <p>
 * Unused ids are {@code null}. The client builds the frontend path from
 * {@code kind} plus the filled ids. This is not a stored {@code href}.
 *
 * @param kind where the client should navigate
 * @param reportId set when {@code kind} is {@code REPORT_QUEUE}
 * @param wallSectionId set for wall/problem/discussion clicks
 * @param problemId set for problem/discussion clicks
 * @param discussionId set for {@code PROBLEM_DISCUSSION} clicks
 * @param userId set for {@code ACCOUNT} clicks
 */
public record NotificationClickDTO(
    NotificationClickKind kind,
    Long reportId,
    Long wallSectionId,
    Long problemId,
    Long discussionId,
    Long userId
) {
}
