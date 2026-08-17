package app.VBeta.api.dto.report;

import java.util.List;

/**
 * One admin-queue case: a single reported target with category tallies and score.
 *
 * @param report target snapshot plus OPEN reporters
 * @param categories per-category counts and {@code weight × count} scores
 * @param queueScore sum of {@code categoryScore} values; higher ranks first
 */
public record ReportPriorityDTO (
        ReportDTO report,
        List<CategoryTallyDTO> categories,
        int queueScore
) {}
