package edu.ics499.VBeta.api.dto;

import java.util.List;

public record ClimbingProblemDetailResponse(
        ClimbingProblemResponse climbingProblem,
        String perceiveGrade,
        List<UserCommentData> discussion
) {
}
