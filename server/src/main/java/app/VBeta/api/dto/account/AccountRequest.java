package app.VBeta.api.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating an account.
 *
 * @param username requested username value
 * @param email requested account email address
 */
public record AccountRequest(
        @NotBlank @Size(max = 25) String username,
        @NotBlank @Email @Size(max = 225) String email
) {}
