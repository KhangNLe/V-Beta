package app.VBeta.api.dto.moderation;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.domain.model.moderation.ModerateActionType;

import java.time.Instant;

public record ModerationDTO(
        Long moderationId,
        ReportDTO report,
        UserAccountDTO resolvedBy,
        ModerateActionType decision,
        String adminNote,
        Instant createdAt
) {}
