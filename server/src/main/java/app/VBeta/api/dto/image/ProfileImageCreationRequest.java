package app.VBeta.api.dto.image;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileImageCreationRequest(
        @NotNull
        ImageTargetType targetType,
        @NotNull @Size(min = 1, max = 250)
        String objectFileName,
        @NotNull @Size(min = 1, max = 250)
        String imageUrl,
        Long userId,
        Long wallSectionId,
        Long climbingProblemId
) {}
