package app.VBeta.domain.model.actions;

/**
 * Enumerates secured actions used for role-based authorization decisions.
 */
public enum ActionDefinition {
    /** Permission to create a solution beta entry. */
    CREATE_BETA,
    /** Permission to delete a solution beta entry. */
    DELETE_BETA,
    /** Permission to create a discussion comment. */
    CREATE_COMMENT,
    /** Permission to delete a discussion comment. */
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
    /** Permission to look at moderation reports from users */
    VIEW_REPORTS
}