package app.VBeta.api.dto.report;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionResponse;
import app.VBeta.domain.model.report.ReportTargetType;

import java.util.List;

public record ReportDTO (
        ReportTargetType targetType,
        UserDiscussionData discussion,
        ClimbingProblemResponse climbingProblem,
        WallSectionResponse wallSection,
        UserAccountDTO user,
        List<ReportUserDTO> reporters
) {}
