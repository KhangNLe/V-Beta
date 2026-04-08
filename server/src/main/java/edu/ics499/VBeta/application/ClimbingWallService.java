package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.WallSection;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public WallSectionResponse createNewWallSection(WallSectionCreationRequest request){
        WallSection newWall = wallSectionManager.createNewWallSection(request);
        return new WallSectionResponse(
                newWall.getId(),
                newWall.getWallSectionName(),
                newWall.getWallInfo()
        );
    }

    public void deleteWallSection(Long wallSectionId){
        WallSection wallSection = wallSectionManager.findWallSection(wallSectionId);
        if (wallSection == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Incorrect wall section ID or the wall section does not exist.");
        }

        List<ClimbingProblem> problems = climbingProblemManager.getAllProblemsFromWallSection(wallSection);
        // dereference all climbing problem with a wall and archive their status
        climbingProblemManager.disconnectFromWallSection(problems);
        wallSectionManager.removeWallSection(wallSectionId);
    }

    public void resetWallSection(Long wallSectionId){
        WallSection wallSection = wallSectionManager.findWallSection(wallSectionId);
        if (wallSection == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Incorrect wall section ID or the wall section does not exist.");
        }
        List<ClimbingProblem> problems = climbingProblemManager.getAllActiveProblemFromWallSection(wallSection);
        climbingProblemManager.archiveActiveProblems(problems);
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
