package app.VBeta.api.dto.discussions.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentDeletionRequest(
        @NotNull
        Long authorId,
        @NotNull
        Long problemId,
        @NotNull
        Long discussionId,
        @NotBlank @Size(max = 250)
        String commentContent
) {
}
