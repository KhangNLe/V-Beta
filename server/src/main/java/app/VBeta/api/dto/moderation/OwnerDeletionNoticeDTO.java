package app.VBeta.api.dto.moderation;

import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.report.ReportStatus;

/**
 * Owner deletion notice for {@code /appeals?reportId=}.
 * <p>
 * Authenticated. The caller must own the reported discussion. Reporter identity
 * is omitted. When the owner has submitted an appeal, {@code appeal} is a nested
 * {@link AppealDTO} using the same stripped snapshot.
 *
 * @param reportId report named in the notification deep-link
 * @param reportStatus current report status
 * @param adminReason notes from the {@code CONTENT_REMOVED} logbook row
 * @param report discussion/problem/wall snapshot plus category/reason; {@code reporters[].reporter} is omitted
 * @param appealStatus existing appeal status, or {@code null} when none
 * @param canAppeal {@code true} when the owner may still submit one appeal
 * @param appeal submitted appeal loadout, or {@code null} when none
 */
public record OwnerDeletionNoticeDTO(
        Long reportId,
        ReportStatus reportStatus,
        String adminReason,
        ReportDTO report,
        AppealStatus appealStatus,
        boolean canAppeal,
        AppealDTO appeal
) {}
