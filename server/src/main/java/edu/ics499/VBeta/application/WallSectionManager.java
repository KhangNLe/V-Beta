package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.PerceiveGradeCalculator;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.WallSectionRepository;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.util.*;

@Service
@Transactional
public class WallSectionManager {
    private final ClimbingProblemRepository climbingProblemRepository;
    private final WallSectionRepository wallSectionRepository;
    private final PerceiveGradeCalculator perceiveGradeCalculator;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    public WallSectionManager(
            ClimbingProblemRepository climbingProblemRepository,
            WallSectionRepository wallSectionRepository,
            PerceiveGradeCalculator perceiveGradeCalculator,
            ClimbingProblemDiscussionManager climbingProblemDiscussionManager
    ){
        this.climbingProblemRepository = climbingProblemRepository;
        this.wallSectionRepository = wallSectionRepository;
        this.perceiveGradeCalculator = perceiveGradeCalculator;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
    }

    public List<WallSectionResponse> getWallSections(){
        List<WallSection> wallSections = wallSectionRepository.findAll();
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
        ClimbingProblem problem = findClimbingProblem(problemId);
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
        WallSection wallSection = findWallSection(wallSectionId);
        return mapProblemsForWall(wallSection);
    }

    private WallSection findWallSection(Long wallSectionId){
        return wallSectionRepository
                .findById(wallSectionId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Wall section with id %d is not found.\n", wallSectionId)
                ));
    }

    private ClimbingProblem findClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemRepository
                .findById(problemId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Problem with id %d is not longer exist.\n", problemId)
                ));

        if (problem.getProblemStatus().equals(LifecycleStatus.ARCHIVE)){
            throw new IllegalStateException(
                    String.format("Problem %s on %s is no longer active.\n",
                            problem.getProblemInfo(), problem.getWallSection().getWallSectionName())
            );
        }

        return problem;
    }

    private List<ClimbingProblemResponse> mapProblemsForWall(WallSection wallSection) {
        List<ClimbingProblemResponse> problemsInfo = new ArrayList<>();
        List<ClimbingProblem> problems = climbingProblemRepository.findByWallSection(wallSection);
        problems.stream()
            .filter(problem -> problem.getProblemStatus().equals(LifecycleStatus.ACTIVE))
            .forEach(problem -> {
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
