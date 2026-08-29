package app.VBeta.api.dto.moderation;

import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.report.ReportStatus;

/**
 * Owner-facing deletion notice for {@code /appeals?reportId=}.
 * <p>
 * Authenticated only. The caller must own the reported discussion.
 *
 * @param reportId report named in the notification deep-link
 * @param reportStatus current report status
 * @param adminReason notes from the {@code CONTENT_REMOVED} logbook row
 * @param report discussion/problem/wall snapshot plus category/reason; {@code reporters[].reporter} is omitted
 * @param appealStatus existing appeal status, or {@code null} when none
 * @param canAppeal {@code true} when the owner may still submit one appeal
 */
public record OwnerDeletionNoticeDTO(
        Long reportId,
        ReportStatus reportStatus,
        String adminReason,
        ReportDTO report,
        AppealStatus appealStatus,
        boolean canAppeal
) {}
