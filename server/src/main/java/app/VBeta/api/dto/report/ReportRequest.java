package app.VBeta.api.dto.report;

import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record ReportRequest (
        @NotNull ReportTargetType reportTargetType,
        @NotBlank @Size(max=250) String reportReason,
        @NotNull ReportCategoryName reportCategoryName,
        @NotNull Long targetId
)  {}
