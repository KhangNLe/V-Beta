package app.VBeta.api.dto.discussions.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for soft-deleting a previously uploaded beta solution video.
 * <p>
 * The discussion root is marked deleted. The solution-beta metadata row and GCS
 * object are kept until a later purge flow.
 *
 * @param userId identifier of the beta author
 * @param problemId identifier of the related climbing problem
 * @param discussionId identifier of the {@code DiscussionRoot} to mark deleted
 * @param publicUrl public URL of the video used for consistency check
 * @param deleteReason reason stored on the discussion root (max 100 characters)
 */
public record SolutionBetaDeletionRequest(
        @NotNull Long userId,
        @NotNull Long problemId,
        @NotNull Long discussionId,
        @NotBlank @Size(max = 250) String publicUrl,
        @NotBlank @Size(max = 100) String deleteReason
) {
}
