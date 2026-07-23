package app.VBeta.api.dto.account;

/**
 * Response DTO for the current authenticated account endpoint.
 *
 * @param userId unique identifier of the authenticated user
 * @param username username of the authenticated user
 * @param email email address of the authenticated user
 * @param role role name assigned to the user, when available
 */
public record AccountMeResponse(Long userId, String username, String email, String role) {}
