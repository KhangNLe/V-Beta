package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequest(
        @NotBlank @Size(max = 25) String username,
        @NotBlank @Email @Size(max = 225) String email
) {}
