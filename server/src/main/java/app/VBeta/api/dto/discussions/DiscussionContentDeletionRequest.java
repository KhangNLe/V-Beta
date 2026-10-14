package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiscussionContentDeletionRequest(
        @NotNull Long authorId,
        @NotNull Long discussionId,
        @NotNull Long problemId,
        @NotBlank @Size(max = 250) String commentContent,
        @NotBlank @Size(max = 250) String videoURL,
        @NotBlank @Size(max = 100) String deleteReason
) {
}
