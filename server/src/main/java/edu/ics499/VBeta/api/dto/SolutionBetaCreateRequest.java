package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolutionBetaCreateRequest(
        @NotBlank
        Long problemId,
        @NotBlank @Size(max = 125)
        String objectFileName,
        @NotBlank @Size(max = 150)
        String videoURL
){}
