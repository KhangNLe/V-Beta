package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for submitting a user's perceived grade for a problem.
 *
 * @param perceiveGrade grade value selected by the user
 */
public record PerceiveGradeRequest(
        @NotNull
        GradeDefinition perceiveGrade
) {
}
