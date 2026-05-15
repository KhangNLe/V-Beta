package app.VBeta.domain.model;

/**
 * Represents lifecycle state for entities that can be active or archived.
 */
public enum LifecycleStatus {
    /** Entity is active and visible in primary workflows. */
    ACTIVE,
    /** Entity is archived and excluded from active workflows. */
    ARCHIVE
}
