package app.VBeta.domain.model.report;

/**
 * Enumerates closed-workflow statuses for a {@link Report}.
 */
public enum ReportStatus {
    /** Awaiting admin review. */
    OPEN,
    /** Admin dismissed the report. */
    DISMISSED,
    /** Reported content was removed. */
    CONTENT_REMOVED,
    /** Content owner submitted an appeal. */
    APPEAL_PENDING,
    /** Admin restored content after appeal. */
    CONTENT_RESTORED,
    /** Admin denied the appeal. */
    APPEAL_DENIED
}
