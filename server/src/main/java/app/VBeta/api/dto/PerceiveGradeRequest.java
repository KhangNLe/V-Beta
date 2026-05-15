package app.VBeta.api.dto;

import app.VBeta.domain.model.GradeDefinition;
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
