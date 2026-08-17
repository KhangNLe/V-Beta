package app.VBeta.api.dto.report;

import java.util.List;

/**
 * Response body for {@code GET /api/report/reports}.
 *
 * @param reports ranked OPEN cases; empty when the queue is empty or the
 *        requested case is hidden from the viewer
 */
public record ReportsPayload(
        List<ReportPriorityDTO> reports
) {}
