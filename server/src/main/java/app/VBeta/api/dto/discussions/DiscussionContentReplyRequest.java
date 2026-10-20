package app.VBeta.api.dto.discussions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a reply under an existing discussion root.
 * <p>
 * Content kind is inferred from nullable fields:
 * <ul>
 *   <li>comment only → comment reply</li>
 *   <li>{@code objectFileName} + {@code videoURL} only → beta reply</li>
 *   <li>comment plus both video fields → mixed reply</li>
 * </ul>
 * At least one content kind must be present; video fields must be supplied together.
 *
 * @param parentDiscussionId discussion being replied to (top-level or another reply)
 * @param problemId climbing problem that owns the parent discussion
 * @param replyToId user id of the parent discussion author
 * @param commentInfo optional reply text (max 250)
 * @param objectFileName optional GCS object key for an uploaded beta (max 250)
 * @param videoURL optional public beta URL (max 250)
 */
public record DiscussionContentReplyRequest(
        @NotNull Long parentDiscussionId,
        @NotNull Long problemId,
        @NotNull Long replyToId,
        @Size(max = 250) String commentInfo,
        @Size(max = 250) String objectFileName,
        @Size(max = 250) String videoURL
) {
}
