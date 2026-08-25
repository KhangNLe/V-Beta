package app.VBeta.api.dto.moderation;

import java.util.List;

/**
 * Admin appeal-queue response: zero or more {@link AppealDTO} rows.
 *
 * @param appeals newest-first OPEN queue, or a single-item list for {@code ?appealId=}
 */
public record AppealPayload(
        List<AppealDTO> appeals
) {}
