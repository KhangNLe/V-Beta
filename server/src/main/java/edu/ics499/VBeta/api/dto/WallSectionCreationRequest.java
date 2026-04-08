package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WallSectionCreationRequest(
        @NotBlank @Size(max = 250)
        String wallSectionInfo,

        @NotBlank @Size(max = 30)
        String wallSectionName
) {
}
