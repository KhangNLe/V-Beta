package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create payload for a mixed discussion post ({@code DiscussionType.MIXED}).
 * Call after the video has been uploaded via the signed-URL flow.
 *
 * @param problemId target climbing problem
 * @param commentInfo text body (max 250)
 * @param objectFileName GCS object key for the uploaded video
 * @param videoURL public playback URL
 */
public record DiscussionContentRequest(
        @NotNull
        Long problemId,
        @NotBlank @Size(max = 250)
        String commentInfo,
        @NotBlank @Size(max = 250)
        String objectFileName,
        @NotBlank @Size(max = 250)
        String videoURL
) {}
