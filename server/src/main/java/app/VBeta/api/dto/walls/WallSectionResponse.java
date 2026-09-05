package app.VBeta.api.dto.walls;

/**
 * Response DTO representing wall section details.
 *
 * @param wallSectionID unique identifier of the wall section
 * @param wallSectionName display name of the wall section
 * @param wallSectionInfo descriptive information about the wall section
 */
public record WallSectionResponse(
        Long wallSectionID,
        String wallSectionName,
        String wallSectionInfo,
        String imageUrl
) {}
