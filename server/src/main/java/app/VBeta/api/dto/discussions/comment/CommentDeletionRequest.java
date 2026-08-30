package app.VBeta.api.dto.discussions.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for soft-deleting a discussion comment.
 *
 * @param authorId identifier of the comment author
 * @param problemId identifier of the related climbing problem
 * @param discussionId identifier of the {@code DiscussionRoot} to mark deleted
 * @param commentContent expected comment text for consistency check
 * @param deletedReason reason stored on the discussion root (max 100 characters)
 */
public record CommentDeletionRequest(
        @NotNull Long authorId,
        @NotNull Long problemId,
        @NotNull Long discussionId,
        @NotBlank @Size(max = 250) String commentContent,
        @NotBlank @Size(max = 100) String deletedReason
) {
}
