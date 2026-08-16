package app.VBeta.api.dto.report;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.domain.model.report.ReportCategoryName;

import java.time.Instant;

public record ReportUserDTO(
        Long reportId,
        UserAccountDTO reporter,
        ReportCategoryName categoryName,
        String reportReason,
        Instant createdAt
) {}
