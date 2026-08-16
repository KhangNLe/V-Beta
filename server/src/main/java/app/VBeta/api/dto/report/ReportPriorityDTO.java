package app.VBeta.api.dto.report;

import java.util.List;

public record ReportPriorityDTO (
        ReportDTO report,
        List<CategoryTallyDTO> categories,
        int queueScore
) {}
