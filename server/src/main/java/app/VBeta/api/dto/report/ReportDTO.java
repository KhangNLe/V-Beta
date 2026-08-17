package app.VBeta.api.dto.report;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionResponse;
import app.VBeta.domain.model.report.ReportTargetType;

import java.util.List;

/**
 * Grouped report case for one typed target.
 * <p>
 * Exactly one of {@code discussion}, {@code climbingProblem}, {@code wallSection},
 * or {@code user} is non-null, matching {@code targetType}.
 *
 * @param targetType reported entity kind
 * @param discussion comment or beta snapshot when {@code targetType} is {@code DISCUSSION}
 * @param climbingProblem problem snapshot when {@code targetType} is {@code CLIMBING_PROBLEM}
 * @param wallSection wall snapshot when {@code targetType} is {@code WALL_SECTION}
 * @param user reported account when {@code targetType} is {@code USER_ACCOUNT}
 * @param reporters OPEN flags on this target (reporter, category, reason, time)
 */
public record ReportDTO (
        ReportTargetType targetType,
        UserDiscussionData discussion,
        ClimbingProblemResponse climbingProblem,
        WallSectionResponse wallSection,
        UserAccountDTO user,
        List<ReportUserDTO> reporters
) {}
