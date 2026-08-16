package app.VBeta.api.dto.report;

import java.util.List;

public record ReportsPayload(
        List<ReportPriorityDTO> reports
) {}
