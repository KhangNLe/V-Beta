package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Soft-delete payload for a mixed discussion post.
 * Both {@code commentContent} and {@code videoURL} must match the stored child rows.
 *
 * @param authorId discussion author user id
 * @param discussionId discussion root id
 * @param problemId climbing problem id
 * @param commentContent expected comment text
 * @param videoURL expected public beta URL
 * @param deleteReason reason stored on the root (max 100)
 */
public record DiscussionContentDeletionRequest(
        @NotNull Long authorId,
        @NotNull Long discussionId,
        @NotNull Long problemId,
        @NotBlank @Size(max = 250) String commentContent,
        @NotBlank @Size(max = 250) String videoURL,
        @NotBlank @Size(max = 100) String deleteReason
) {
}
