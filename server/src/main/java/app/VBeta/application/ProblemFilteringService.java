package app.VBeta.application;

import app.VBeta.api.dto.ClimbingProblemResponse;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.GradeDefinition;
import app.VBeta.domain.model.WallSection;
import app.VBeta.application.support.grade.ClimbingGradeManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProblemFilteringService {
    private final WallSectionManager wallSectionManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final ClimbingGradeManager climbingGradeManager;

    public  ProblemFilteringService(WallSectionManager wallSectionManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingGradeManager climbingGradeManager){
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
        this.climbingGradeManager = climbingGradeManager;
    }

    public List<ClimbingProblemResponse> findProblemsByGrade(Long wallSectionId, GradeDefinition targetGrade){
        WallSection wall = wallSectionManager.findWallSection(wallSectionId);
        validateWallSection(wall);

        return null;
    }

    private void validateWallSection(WallSection wall){
        if (wall == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Unable to find the wall section"
            );
        }
    }
}
