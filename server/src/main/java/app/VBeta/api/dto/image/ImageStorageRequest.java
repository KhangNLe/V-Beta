package app.VBeta.api.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for minting a signed image upload URL.
 * <p>
 * Bound from query parameters on {@code GET /api/social/image/signed-url}.
 * Required context fields depend on {@link #imageTargetType()}:
 * <ul>
 *   <li>{@link ImageTargetType#WALL_SECTION} — {@code wallSectionId}</li>
 *   <li>{@link ImageTargetType#CLIMBING_PROBLEM} — {@code problemId}</li>
 *   <li>{@link ImageTargetType#USER_ACCOUNT} — {@code userid}</li>
 * </ul>
 *
 * @param fileName original file name (used to derive extension and sanitized object key)
 * @param contentType MIME type that must match the file extension ({@code image/jpeg}, {@code image/png}, {@code image/webp})
 * @param imageTargetType upload destination
 * @param problemId climbing problem id when {@code imageTargetType = CLIMBING_PROBLEM}
 * @param wallSectionId wall section id when {@code imageTargetType = WALL_SECTION}
 * @param userid account id when {@code imageTargetType = USER_ACCOUNT}
 */
public record ImageStorageRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull ImageTargetType imageTargetType,
        Long problemId,
        Long wallSectionId,
        Long userid
){}
