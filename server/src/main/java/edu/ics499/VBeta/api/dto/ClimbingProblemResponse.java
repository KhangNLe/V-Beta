package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;

public record ClimbingProblemResponse(
        Long problemId,
        String holdColor,
        String info,
        String createdDate,
        GradeDefinition assignedGrade
) {
}
