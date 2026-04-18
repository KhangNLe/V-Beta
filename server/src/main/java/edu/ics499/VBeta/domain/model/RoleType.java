package edu.ics499.VBeta.domain.model;

/**
 * Enumerates role identities used by gym authorization rules.
 */
public enum RoleType {
    /** Standard climber role with participant-level permissions. */
    CLIMBER,
    /** Setter role with route-management permissions. */
    SETTER,
    /** Administrator role with elevated system permissions. */
    ADMIN
}