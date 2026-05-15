package app.VBeta.api.dto;

/**
 * Response DTO representing account details.
 *
 * @param id unique account identifier
 * @param username account username
 * @param email account email address
 * @param firebaseUid Firebase UID mapped to the account
 * @param roleName role assigned to the account
 */
public record AccountResponse(
        Long id,
        String username,
        String email,
        String firebaseUid,
        String roleName) {}
