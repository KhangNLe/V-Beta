package app.VBeta.domain.model.notification;

/**
 * Enumerates seeded notifiable event kinds.
 */
public enum EventTypeName {
    /** A user submitted a content report. */
    REPORT_CREATED,
    /** An admin dismissed a report the user submitted. */
    REPORT_DISMISSED,
    /** Reported content was removed; sent to the content owner. */
    CONTENT_REMOVED,
    /** A content owner appealed a removal. */
    APPEAL_SUBMITTED,
    /** An admin restored content after appeal. */
    CONTENT_RESTORED,
    /** An admin denied an appeal. */
    APPEAL_DENIED,
    /** Reported content was removed; sent to the reporter. */
    REPORT_APPROVED
}
