package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a beta solution entry for a climbing problem.
 *
 * @param problemId identifier of the climbing problem
 * @param objectFileName storage object name for the uploaded media file
 * @param videoURL public URL for the uploaded video
 */
public record SolutionBetaCreateRequest(
        @NotBlank
        Long problemId,
        @NotBlank @Size(max = 125)
        String objectFileName,
        @NotBlank @Size(max = 150)
        String videoURL
) {}
