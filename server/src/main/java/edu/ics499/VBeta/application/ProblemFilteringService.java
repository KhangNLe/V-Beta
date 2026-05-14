package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.WallSectionManager;
import edu.ics499.VBeta.domain.model.GradeDefinition;
import edu.ics499.VBeta.domain.model.WallSection;
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

    public  ProblemFilteringService(WallSectionManager wallSectionManager,
                                    ClimbingProblemManager climbingProblemManager){
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
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
