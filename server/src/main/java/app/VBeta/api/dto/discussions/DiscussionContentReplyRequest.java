package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiscussionContentReplyRequest(
        @NotNull Long parentDiscussionId,
        @NotNull Long problemId,
        @NotNull Long replyToId,
        @Size(max = 250) String commentInfo,
        @Size(max = 250) String objectFileName,
        @Size(max = 250) String videoURL
) {
}
