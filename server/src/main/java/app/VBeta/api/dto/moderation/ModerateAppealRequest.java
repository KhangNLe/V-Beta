package app.VBeta.api.dto.moderation;

import app.VBeta.domain.model.appeal.AppealStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ModerateAppealRequest(
        @NotNull Long appealId,
        @NotNull AppealStatus appealStatus,
        @NotBlank @Size(max = 255) String adminReason
) {
    public ModerateAppealRequest {
        if (appealStatus == AppealStatus.OPEN) {
            throw new IllegalArgumentException("Invalid appeal status");
        }
    }
}
