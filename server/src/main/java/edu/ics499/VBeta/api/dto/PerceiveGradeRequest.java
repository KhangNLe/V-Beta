package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;
import jakarta.validation.constraints.NotBlank;

public record PerceiveGradeRequest(
        @NotBlank
        GradeDefinition perceiveGrade
) {
}
