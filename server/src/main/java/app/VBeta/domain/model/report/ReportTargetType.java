package app.VBeta.domain.model.report;

/**
 * Enumerates typed report targets. Exactly one matching FK column is set on {@link Report}.
 */
public enum ReportTargetType {
    /** Discussion comment or solution beta. */
    DISCUSSION,
    /** Wall section. */
    WALL_SECTION,
    /** Climbing problem. */
    CLIMBING_PROBLEM,
    /** User account. */
    USER_ACCOUNT
}
