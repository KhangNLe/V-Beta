package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
