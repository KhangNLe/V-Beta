package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.GradeDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new climbing problem.
 *
 * @param holdColor hold color used for the problem route
 * @param info human-readable description of the route
 * @param assignedGrade initial assigned grade for the route
 */
public record ClimbingProblemCreationRequest(
        @NotBlank @Size(max = 25)
        String holdColor,
        @NotBlank @Size(max = 250)
        String info,
        @NotBlank
        GradeDefinition assignedGrade
) {
}
