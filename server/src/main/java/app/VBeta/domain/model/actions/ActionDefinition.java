package app.VBeta.domain.model.actions;

/**
 * Enumerates secured actions used for role-based authorization decisions.
 */
public enum ActionDefinition {
    /** Permission to create a solution beta entry. */
    CREATE_BETA,
    /** Permission to hide (soft-delete) a solution beta entry. Not currently checked on {@code DELETE /api/discussion/solution-beta}. */
    DELETE_BETA,
    /** Permission to create a discussion comment. */
    CREATE_COMMENT,
    /** Permission to hide (soft-delete) a discussion comment. Required on {@code DELETE /api/discussion/comment/delete}. */
    DELETE_COMMENT,
    /** Permission to create a climbing problem. */
    CREATE_PROBLEM,
    /** Permission to delete a climbing problem. */
    DELETE_PROBLEM,
    /** Permission to reset/archive a wall section's active problems. */
    RESET_WALL,
    /** Permission to create a wall section. */
    CREATE_WALL,
    /** Permission to delete a wall section. */
    DELETE_WALL,
    /** Permission to change another user's role. */
    CHANGE_ROLE,
    /** Permission to view all user accounts. */
    VIEW_ACCOUNTS,
    /** Permission to submit perceived grades for problems. */
    GRADE_PROBLEM,
    /** Permission to view the admin report queue and report detail. */
    VIEW_REPORTS,
    /** Permission to dismiss or remove reported discussion content ({@code POST /api/moderate/report}). */
    MODERATE_REPORT,
    /** Permission to read the append-only moderation logbook ({@code GET /api/moderate/logbook}). */
    VIEW_MODERATION_LOGS,
    /** Permission to read the admin appeal queue and appeal detail ({@code GET /api/moderate/appeal}). */
    VIEW_APPEALS,
    /** Permission to approve or deny an appeal ({@code PATCH /api/moderate/appeal}). */
    MODERATE_APPEAL,
    /** Permission to upload or delete a wall section image ({@code /api/social/image/*} with {@code WALL_SECTION}). */
    UPLOAD_WALL_IMAGE,
    /** Permission to upload or delete a climbing problem image ({@code /api/social/image/*} with {@code CLIMBING_PROBLEM}). */
    UPLOAD_PROBLEM_IMAGE,
    UPDATE_CLIMB_INFO,
    UPDATE_WALL_INFO
}