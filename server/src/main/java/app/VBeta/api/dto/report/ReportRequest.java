package app.VBeta.api.dto.report;

import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a content report.
 *
 * @param reportTargetType typed target kind ({@code DISCUSSION}, {@code WALL_SECTION},
 *        {@code CLIMBING_PROBLEM}, or {@code USER_ACCOUNT})
 * @param reportReason reporter-supplied reason, at most 250 characters
 * @param reportCategoryName catalog category for queue ranking
 * @param targetId identifier of the concrete target row
 */
public record ReportRequest (
        @NotNull ReportTargetType reportTargetType,
        @NotBlank @Size(max=250) String reportReason,
        @NotNull ReportCategoryName reportCategoryName,
        @NotNull Long targetId
)  {}
