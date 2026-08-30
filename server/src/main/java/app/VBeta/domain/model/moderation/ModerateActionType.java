package app.VBeta.domain.model.moderation;

/**
 * Enumerates admin logbook action kinds recorded on a report.
 * <p>
 * {@code POST /api/moderate/report} accepts {@link #REPORT_DISMISSED} and
 * {@link #CONTENT_REMOVED} only. Appeal types are persisted in the catalog for a
 * later endpoint.
 */
public enum ModerateActionType {
    /** Admin dismissed the report. Notifies the reporter only. */
    REPORT_DISMISSED,
    /** Admin removed the reported discussion. Notifies reporter and owner. */
    CONTENT_REMOVED,
    /** Admin approved an appeal. Not accepted on the report-queue endpoint. */
    APPEAL_APPROVED,
    /** Admin denied an appeal. Not accepted on the report-queue endpoint. */
    APPEAL_DENIED
}
