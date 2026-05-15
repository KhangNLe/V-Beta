package app.VBeta.api.dto;

import java.util.List;

/**
 * Detailed response for a climbing problem, including discussion context.
 *
 * @param climbingProblem core climbing problem data
 * @param perceiveGrade aggregated or calculated perceived grade label
 * @param discussion ordered discussion comments for the problem
 */
public record ClimbingProblemDetailResponse(
        ClimbingProblemResponse climbingProblem,
        String perceiveGrade,
        List<UserCommentData> discussion
) {
}
