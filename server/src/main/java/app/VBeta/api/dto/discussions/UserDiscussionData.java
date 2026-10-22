package app.VBeta.api.dto.discussions;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response DTO describing one discussion item in a problem thread.
 * <p>
 * Kind is inferred from payloads: {@code comment} only, {@code videoURL} only,
 * or both for a mixed post. Either field may be {@code null}. Soft-deleted roots
 * stay on the timeline with {@code isDeleted = true} and a placeholder {@code comment}.
 *
 * @param discussionId identifier of the discussion root row
 * @param userId identifier of the authoring user
 * @param username display name of the authoring user
 * @param parentDiscussionId parent discussion id for replies; {@code null} for top-level posts
 * @param comment nullable text body from {@code Discussion_Comment}
 * @param videoURL nullable public beta URL from {@code Solution_Beta}
 * @param createdDate timestamp when the discussion root was created
 * @param isDeleted {@code true} when the root is soft-deleted (placeholder row)
 */
public record UserDiscussionData(
        Long discussionId,
        Long userId,
        String username,
        Long parentDiscussionId,
        String comment,
        String videoURL,
        LocalDateTime createdDate,
        @JsonProperty("isDeleted") Boolean isDeleted
) {
}
