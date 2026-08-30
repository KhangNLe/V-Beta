package app.VBeta.domain.model.appeal;

/**
 * Enumerates appeal review outcomes.
 */
public enum AppealStatus {
    /** Awaiting admin review. */
    OPEN,
    /** Admin approved restore. */
    APPROVED,
    /** Admin denied the appeal. */
    DENIED
}
