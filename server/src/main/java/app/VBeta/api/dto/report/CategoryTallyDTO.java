package app.VBeta.api.dto.report;

import app.VBeta.domain.model.report.ReportCategoryName;

/**
 * Category breakdown on one report case.
 *
 * @param categoryName seeded report category
 * @param reportCount number of OPEN reports in this category on the target
 * @param categoryScore {@code weight × reportCount}
 */
public record CategoryTallyDTO(
        ReportCategoryName categoryName,
        int reportCount,
        int categoryScore
) {
}
