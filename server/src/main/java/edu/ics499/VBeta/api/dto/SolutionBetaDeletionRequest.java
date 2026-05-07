package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for deleting a previously uploaded beta solution video.
 *
 * @param userId identifier of the user requesting deletion
 * @param problemId identifier of the related climbing problem
 * @param publicUrl public URL of the video to remove
 */
public record SolutionBetaDeletionRequest(
        @NotNull
        Long userId,
        @NotNull
        Long problemId,
        @NotNull
        Long discussionId,
        @NotBlank @Size(max = 250)
        String publicUrl
) {
}
