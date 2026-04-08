package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;

public record PerceiveGradeRequest(
        GradeDefinition perceiveGrade
) {
}
