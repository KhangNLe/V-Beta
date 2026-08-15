package app.VBeta.domain.model.report;

/**
 * Enumerates seeded report categories used for queue ranking.
 */
public enum ReportCategoryName {
    /** Highest-priority queue category. */
    INAPPROPRIATE_CONTENT,
    /** Harassment or bullying reports. */
    HARASSMENT_BULLYING,
    /** Unsolicited or repetitive content. */
    SPAM,
    /** Content unrelated to the discussion or gym context. */
    OFF_TOPIC
}
