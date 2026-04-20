package edu.ics499.VBeta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentDeletionRequest(
        @NotNull
        Long authorId,
        @NotNull
        Long problemId,
        @NotBlank @Size(max = 250)
        String commentContent
) {
}
