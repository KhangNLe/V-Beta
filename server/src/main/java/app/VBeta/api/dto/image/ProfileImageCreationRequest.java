package app.VBeta.api.dto.image;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for persisting image metadata after a client-side GCS upload.
 * <p>
 * Bound from query parameters on {@code PATCH /api/social/image/upload}.
 * Provide the entity id that matches {@link #targetType()}.
 *
 * @param targetType entity receiving the image metadata
 * @param objectFileName GCS object key returned from the signed-url step
 * @param imageUrl public URL for client display
 * @param userId account id when {@code targetType = USER_ACCOUNT}
 * @param wallSectionId wall section id when {@code targetType = WALL_SECTION}
 * @param climbingProblemId problem id when {@code targetType = CLIMBING_PROBLEM}
 */
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
