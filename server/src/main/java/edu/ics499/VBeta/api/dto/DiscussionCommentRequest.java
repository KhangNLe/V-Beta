package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a discussion comment on a climbing problem.
 *
 * @param problemId identifier of the climbing problem being discussed
 * @param commentInfo comment text submitted by the user
 */
public record DiscussionCommentRequest(
        @NotNull
        Long problemId,
        @NotBlank @Size(max = 250)
        String commentInfo
) {
}
