package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscussionCommentRequest(
        @NotBlank
        Long problemId,
        @NotBlank @Size(max = 250) String commentInfo
) {
}
