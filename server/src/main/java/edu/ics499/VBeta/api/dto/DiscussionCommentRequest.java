package edu.ics499.VBeta.api.dto;

public record DiscussionCommentRequest(
        Long userId,
        Long problemId,
        String commentInfo
) {
}
