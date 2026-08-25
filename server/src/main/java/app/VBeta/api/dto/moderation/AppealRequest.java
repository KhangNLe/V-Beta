package app.VBeta.api.dto.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Owner request to appeal a {@code CONTENT_REMOVED} discussion report.
 *
 * @param reportId report that closed as content-removed
 * @param appealReason owner-supplied restore reason (max 250 characters)
 */
public record AppealRequest(
        @NotNull Long reportId,
        @NotBlank @Size(max = 250) String appealReason
) {}
