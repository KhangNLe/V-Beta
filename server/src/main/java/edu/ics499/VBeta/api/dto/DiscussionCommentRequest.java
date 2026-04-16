package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiscussionCommentRequest(
        @NotNull
        Long problemId,
        @NotNull @Size(max = 250)
        String commentInfo
) {
}
