package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClimbingProblemCreationRequest(
        @NotBlank @Size(max = 25)
        String holdColor,
        @Size(max = 250)
        String info,
        @NotBlank
        GradeDefinition assignedGrade
) {
}
