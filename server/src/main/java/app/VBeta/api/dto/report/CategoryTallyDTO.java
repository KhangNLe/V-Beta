package app.VBeta.api.dto.report;

import app.VBeta.domain.model.report.ReportCategoryName;

public record CategoryTallyDTO(
        ReportCategoryName categoryName,
        int reportCount,
        int categoryScore
) {
}
