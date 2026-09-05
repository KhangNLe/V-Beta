package app.VBeta.api.dto.problems;

import app.VBeta.domain.model.climb.GradeDefinition;

/**
 * Response DTO representing a climbing problem summary.
 *
 * @param problemId unique identifier of the climbing problem
 * @param holdColor hold color used for the route
 * @param info user-facing problem description
 * @param createdDate timestamp string when the problem was created
 * @param assignedGrade assigned grade of the climbing problem
 */
public record ClimbingProblemResponse(
        Long problemId,
        String holdColor,
        String info,
        String createdDate,
        GradeDefinition assignedGrade,
        String imageUrl
) {
}
