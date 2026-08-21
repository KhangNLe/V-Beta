package app.VBeta.api.dto.moderation;

import java.util.List;

/**
 * Admin logbook response: zero or more {@link ModerationDTO} rows.
 *
 * @param moderationLogs newest-first page, or a single-item list for {@code ?moderationId=}
 */
public record ModerationPayload(
        List<ModerationDTO> moderationLogs
) {
}
