package app.VBeta.api.dto.notification;

/**
 * Client navigation kind for an inbox click.
 * <p>
 * Distinct from {@link app.VBeta.domain.model.notification.EventTargetType}:
 * current moderation events target a {@code REPORT}, so they map to
 * {@link #REPORT_QUEUE}. Other values are reserved for later event targets.
 */
public enum NotificationClickKind {
    /** Admin report queue/detail ({@code GET /api/report/reports?reportId=}). */
    REPORT_QUEUE,
    /** Problem page focused on a discussion ({@code /wall/{wall}/problem/{problem}?discussionId=}). */
    PROBLEM_DISCUSSION,
    /** Problem page without a discussion focus. */
    PROBLEM,
    /** Wall section page. */
    WALL_SECTION,
    /** User account page. */
    ACCOUNT
}
