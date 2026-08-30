package app.VBeta.api.dto.moderation;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.report.ReportDTO;

/**
 * One appeal row returned to an admin.
 *
 * @param appealId appeal identifier
 * @param report snapshot of the report being appealed (that reporter row only)
 * @param appealUser content owner who submitted the appeal
 * @param appealReason owner-supplied reason for restore
 */
public record AppealDTO(
        Long appealId,
        ReportDTO report,
        UserAccountDTO appealUser,
        String appealReason
) {}
