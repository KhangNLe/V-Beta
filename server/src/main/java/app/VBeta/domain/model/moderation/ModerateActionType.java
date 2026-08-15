package app.VBeta.domain.model.moderation;

/**
 * Enumerates admin logbook action kinds recorded on a report.
 */
public enum ModerateActionType {
    /** Admin dismissed the report. */
    REPORT_DISMISSED,
    /** Admin removed the reported content. */
    CONTENT_REMOVED,
    /** Admin approved an appeal. */
    APPEAL_APPROVED,
    /** Admin denied an appeal. */
    APPEAL_DENIED
}
