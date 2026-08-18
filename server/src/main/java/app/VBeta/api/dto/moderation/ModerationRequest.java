package app.VBeta.api.dto.moderation;

import app.VBeta.domain.model.moderation.ModerateActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ModerationRequest(
        @NotNull List<Long> reportIds,
        @NotNull ModerateActionType decision,
        @NotEmpty String reason
) {}
