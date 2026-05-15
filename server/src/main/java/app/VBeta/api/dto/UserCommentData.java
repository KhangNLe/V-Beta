package app.VBeta.api.dto;

import app.VBeta.domain.model.DiscussionType;

import java.time.LocalDateTime;

/**
 * Response DTO describing one discussion item (comment or beta) in a problem thread.
 *
 * @param discussionId identifier of the discussion root row
 * @param userId identifier of the commenting user
 * @param username display name of the commenting user
 * @param parentCommentId optional parent discussion id for replies
 * @param discussionType discussion kind (COMMENT or BETA)
 * @param discussionContent comment text or beta URL
 * @param createdDate timestamp when the comment was created
 */
public record UserCommentData(
        Long discussionId,
        Long userId,
        String username,
        Long parentCommentId,
        DiscussionType discussionType,
        String discussionContent,
        LocalDateTime createdDate
) {
}
