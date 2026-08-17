package app.VBeta.domain.model.report;

/**
 * Enumerates seeded report categories used for queue scoring.
 * <p>
 * Catalog {@code weight} (higher is more severe): inappropriate 4, harassment 3,
 * spam 2, off-topic 1.
 */
public enum ReportCategoryName {
    /** Highest-weight queue category. */
    INAPPROPRIATE_CONTENT,
    /** Harassment or bullying reports. */
    HARASSMENT_BULLYING,
    /** Unsolicited or repetitive content. */
    SPAM,
    /** Content unrelated to the discussion or gym context. */
    OFF_TOPIC
}
