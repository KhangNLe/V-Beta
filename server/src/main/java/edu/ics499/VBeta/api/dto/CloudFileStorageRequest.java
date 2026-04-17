package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CloudFileStorageRequest(
        @NotBlank
        String fileName,
        @NotBlank
        String contentType,
        @NotBlank
        Long problemId,
        @NotBlank
        Long wallSectionId
) {
}
