package app.VBeta.api.dto.discussions.video;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for preparing a cloud storage upload for solution media.
 *
 * @param fileName original file name to be uploaded
 * @param contentType MIME type of the upload
 * @param problemId related climbing problem identifier
 * @param wallSectionId related wall section identifier
 */
public record CloudFileStorageRequest(
        @NotBlank
        String fileName,
        @NotBlank
        String contentType,
        @NotBlank
        Long problemId,
        @NotBlank
        Long wallSectionId
) {
}
