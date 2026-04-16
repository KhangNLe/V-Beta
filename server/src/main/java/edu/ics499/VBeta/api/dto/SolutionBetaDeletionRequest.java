package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolutionBetaDeletionRequest(
        @NotBlank
        Long userId,
        @NotBlank
        Long problemId,
        @NotBlank @Size(max = 150)
        String publicUrl
) {
}
