package app.VBeta.api.dto.discussions;

import java.time.LocalDateTime;

/**
 * Response DTO describing one discussion item (comment or beta) in a problem thread.
 *
 * @param discussionId identifier of the discussion root row
 * @param userId identifier of the commenting user
 * @param username display name of the commenting user
 * @param parentCommentId optional parent discussion id for replies
 * @param createdDate timestamp when the comment was created
 */
public record UserDiscussionData(
        Long discussionId,
        Long userId,
        String username,
        Long parentCommentId,
        String comment,
        String videoURL,
        LocalDateTime createdDate
) {
}
