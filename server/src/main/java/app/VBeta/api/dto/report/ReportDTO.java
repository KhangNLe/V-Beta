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
 * The snapshot matching {@code targetType} is always set. Discussion cases
 * also include {@code climbingProblem} and {@code wallSection} from the
 * discussion's problem so the admin queue can show wall/problem context.
 * Problem cases also include {@code wallSection}.
 *
 * @param targetType reported entity kind
 * @param discussion comment or beta snapshot when {@code targetType} is {@code DISCUSSION}
 * @param climbingProblem problem snapshot for discussion and problem targets
 * @param wallSection wall snapshot for discussion, problem, and wall targets
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
