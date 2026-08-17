package app.VBeta.api.dto.moderation;

import app.VBeta.domain.model.moderation.ModerateActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ModerationRequest(
        @NotNull Long reportId,
        @NotNull ModerateActionType decision,
        @NotEmpty String reason
) {}
