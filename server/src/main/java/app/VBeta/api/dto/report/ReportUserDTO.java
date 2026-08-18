package app.VBeta.api.dto.report;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.domain.model.report.ReportCategoryName;

import java.time.Instant;

/**
 * One OPEN report row on a grouped case.
 *
 * @param reportId persisted report identifier
 * @param reporter account that submitted the flag
 * @param categoryName category chosen by the reporter
 * @param reportReason reporter-supplied text
 * @param createdAt when the report was created
 */
public record ReportUserDTO(
        Long reportId,
        UserAccountDTO reporter,
        ReportCategoryName categoryName,
        String reportReason,
        Instant createdAt
) {}
