package app.VBeta.api.dto.moderation;

import java.util.List;

public record ModerationPayload(
        List<ModerationDTO> moderationLogs
) {
}
