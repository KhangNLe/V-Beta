package edu.ics499.VBeta.api.dto;

public record ProblemDetailResponse(
        Long id,
        String name,
        String description,
        String assignedGrade,
        String communityGrade
) {
}
