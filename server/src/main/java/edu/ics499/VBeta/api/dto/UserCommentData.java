package edu.ics499.VBeta.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO describing a user's comment in a problem discussion thread.
 *
 * @param userId identifier of the commenting user
 * @param username display name of the commenting user
 * @param comment comment text
 * @param videoURL optional URL to the associated beta video
 * @param createdDate timestamp when the comment was created
 */
public record UserCommentData(
        Long userId,
        String username,
        String comment,
        String videoURL,
        LocalDateTime createdDate
) {
}
