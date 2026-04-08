package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.WallSectionRepository;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class ClimbingWallService {
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final WallSectionManager wallSectionManager;
    private final ClimbingProblemDeletionManager climbingProblemDeletionManager;
    private final UserPerceiveGradeManager userPerceiveGradeManager;

    public ClimbingWallService(
            ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
            ClimbingProblemManager climbingProblemManager,
            WallSectionManager wallSectionManager,
            ClimbingProblemDeletionManager climbingProblemDeletionManager,
            UserPerceiveGradeManager userPerceiveGradeManager
    ){
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
        this.climbingProblemDeletionManager = climbingProblemDeletionManager;
        this.userPerceiveGradeManager = userPerceiveGradeManager;
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
        ClimbingProblem problem = getActiveProblem(problemId);

        String perceiveGrade = userPerceiveGradeManager.getPerceiveGrade(problem);
        List<UserCommentData> comments = climbingProblemDiscussionManager.getCommentsForProblem(problem);
        return new ClimbingProblemDetailResponse(
                new ClimbingProblemResponse(problem.getId(),
                        problem.getHoldColor(),
                        problem.getProblemInfo(),
                        problem.getCreatedDate().toString().split("T")[0],
                        problem.getClimbingGrade().getGradeDefinition()),
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
                        problem.getClimbingGrade().getGradeDefinition()
                ));
        });
        return problemsInfo;
    }

    public ClimbingProblemResponse createNewClimbingProblem(Long wallSectionId, ClimbingProblemCreationRequest request){
        WallSection wall = wallSectionManager.findWallSection(wallSectionId);
        ClimbingProblem newProblem = climbingProblemManager.generateNewClimbingProblem(wall, request);
        return new ClimbingProblemResponse(
                newProblem.getId(),
                newProblem.getHoldColor(),
                newProblem.getProblemInfo(),
                newProblem.getCreatedDate().toString().split("T")[0],
                newProblem.getClimbingGrade().getGradeDefinition()
        );
    }

    public void deleteClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        climbingProblemDeletionManager.deleteClimbingProblem(problem);
    }

    public void addClimbingProblemPerceiveGrade(String firebaseUid, Long problemId, PerceiveGradeRequest request){
        ClimbingProblem problem = getActiveProblem(problemId);
        userPerceiveGradeManager.addPerceiveGrade(problem, firebaseUid, request.perceiveGrade());
    }

    private ClimbingProblem getActiveProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Problem does not exist or no longer active.");
        }
        return problem;
    }
}
