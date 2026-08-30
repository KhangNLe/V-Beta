package app.VBeta.api.dto.moderation;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.domain.model.moderation.ModerateActionType;

import java.time.Instant;

/**
 * One append-only moderation logbook row returned to an admin.
 *
 * @param moderationId logbook row identifier
 * @param report snapshot of the report that was decided (that reporter row only)
 * @param resolvedBy admin who wrote the logbook row
 * @param decision {@code REPORT_DISMISSED} or {@code CONTENT_REMOVED} (appeals later)
 * @param adminNote required notes from the resolve request
 * @param createdAt when the logbook row was written
 */
public record ModerationDTO(
        Long moderationId,
        ReportDTO report,
        UserAccountDTO resolvedBy,
        ModerateActionType decision,
        String adminNote,
        Instant createdAt
) {}
