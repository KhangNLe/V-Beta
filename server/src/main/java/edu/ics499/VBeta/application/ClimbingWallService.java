package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.PerceiveGradeCalculator;
import edu.ics499.VBeta.application.support.WallSectionManager;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.WallSectionRepository;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ClimbingWallService {
    private final PerceiveGradeCalculator perceiveGradeCalculator;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final WallSectionManager wallSectionManager;

    public ClimbingWallService(
            PerceiveGradeCalculator perceiveGradeCalculator,
            ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
            ClimbingProblemManager climbingProblemManager,
            WallSectionManager wallSectionManager
    ){
        this.perceiveGradeCalculator = perceiveGradeCalculator;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
    }

    public List<WallSectionResponse> getWallSections(){
        List<WallSection> wallSections = wallSectionManager.getWallSections();
        List<WallSectionResponse> wallSectionInfo = new ArrayList<>();

        wallSections.forEach(section -> {
            wallSectionInfo.add(new WallSectionResponse(
                    section.getId(),
                    section.getWallSectionName(),
                    section.getWallInfo()));
        });

        return wallSectionInfo;
    }

    public ClimbingProblemDetailResponse getClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        String perceiveGrade = perceiveGradeCalculator.findPerceiveGrade(problem);
        List<UserCommentData> comments = climbingProblemDiscussionManager.getCommentsForProblem(problem);
        return new ClimbingProblemDetailResponse(
                new ClimbingProblemResponse(problem.getId(),
                        problem.getHoldColor(),
                        problem.getProblemInfo(),
                        problem.getCreatedDate().toString().split("T")[0],
                        problem.getClimbingGrade().getGrade()),
                perceiveGrade,
                comments
        );
    }

    public List<ClimbingProblemResponse> getClimbingProblemsByWallSectionId(Long wallSectionId) {
        WallSection wallSection = wallSectionManager.findWallSection(wallSectionId);
        return mapProblemsForWall(wallSection);
    }

    private List<ClimbingProblemResponse> mapProblemsForWall(WallSection wallSection) {
        List<ClimbingProblemResponse> problemsInfo = new ArrayList<>();
        List<ClimbingProblem> problems = climbingProblemManager.getAllActiveProblemFromWallSection(wallSection);
        problems.forEach(problem -> {
                problemsInfo.add(new ClimbingProblemResponse(
                        problem.getId(),
                        problem.getHoldColor(),
                        problem.getProblemInfo(),
                        problem.getCreatedDate().toString().split("T")[0],
                        problem.getClimbingGrade().getGrade()
                ));
        });
        return problemsInfo;
    }
}
