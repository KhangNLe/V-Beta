package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentDeletionRequest(
        @NotBlank
        Long authorId,
        @NotBlank
        Long problemId,
        @NotBlank @Size(max = 250)
        String commentContent
) {
}
