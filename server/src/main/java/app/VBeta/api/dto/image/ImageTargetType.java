package app.VBeta.api.dto.image;

/**
 * Identifies which entity an image upload or metadata save targets.
 */
public enum ImageTargetType {
    /** User profile avatar (caller must match the requested user id). */
    USER_ACCOUNT,
    /** Wall section photo (requires {@code UPLOAD_WALL_IMAGE}). */
    WALL_SECTION,
    /** Climbing problem photo (requires {@code UPLOAD_PROBLEM_IMAGE}). */
    CLIMBING_PROBLEM
}
