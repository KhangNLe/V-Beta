package edu.ics499.VBeta.api.dto;

import java.time.LocalDateTime;

public record UserCommentData(
        Long userId,
        String username,
        String comment,
        String videoURL,
        LocalDateTime createdDate
) {
}
