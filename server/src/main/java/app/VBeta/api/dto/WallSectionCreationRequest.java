package app.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new wall section.
 *
 * @param wallSectionInfo descriptive details about the wall section
 * @param wallSectionName display name for the wall section
 */
public record WallSectionCreationRequest(
        @NotBlank @Size(max = 250)
        String wallSectionInfo,

        @NotBlank @Size(max = 30)
        String wallSectionName
) {
}
