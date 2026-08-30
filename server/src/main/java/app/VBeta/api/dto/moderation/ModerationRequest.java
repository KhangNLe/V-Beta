package app.VBeta.api.dto.moderation;

import app.VBeta.domain.model.moderation.ModerateActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for an admin report-queue decision.
 * <p>
 * Each {@code reportIds} value is one reporter row. The client must send every
 * OPEN reporter id it intends to close; omitted siblings stay {@code OPEN}.
 * {@code reason} is stored on {@code Moderation_Action.admin_notes}, not on
 * {@code Events}.
 *
 * @param reportIds OPEN report identifiers to close (unknown/hidden ids are skipped)
 * @param decision {@code REPORT_DISMISSED} or {@code CONTENT_REMOVED};
 *        appeal types are rejected by the service
 * @param reason required admin notes copied onto the logbook row
 */
public record ModerationRequest(
        @NotNull List<Long> reportIds,
        @NotNull ModerateActionType decision,
        @NotEmpty String reason
) {}
