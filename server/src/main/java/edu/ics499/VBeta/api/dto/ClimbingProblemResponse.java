package edu.ics499.VBeta.api.dto;

public record ClimbingProblemResponse(
        Long problemId,
        String holdColor,
        String info,
        String createdDate,
        String assignedGrade
) {
}
