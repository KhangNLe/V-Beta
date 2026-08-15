package app.VBeta.domain.model.notification;

/**
 * Enumerates typed event targets. Exactly one matching FK column is set on {@link Events}.
 */
public enum EventTargetType {
    /** A content report. */
    REPORT,
    /** A discussion comment or beta. */
    DISCUSSION,
    /** A climbing problem. */
    CLIMBING_PROBLEM,
    /** A wall section. */
    WALL_SECTION,
    /** A user account. */
    USER_ACCOUNT
}
