package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.ClimbingProblemCreationRequest;
import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.api.dto.WallSectionCreationRequest;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ClimbingProblemManager {
    private final ClimbingProblemRepository climbingProblemRepository;
    private final ClimbingGradeRepository climbingGradeRepository;

    public ClimbingProblemManager(ClimbingProblemRepository climbingProblemRepository,
                                  ClimbingGradeRepository climbingGradeRepository){
        this.climbingProblemRepository = climbingProblemRepository;
        this.climbingGradeRepository = climbingGradeRepository;
    }

    public ClimbingProblem getActiveProblem(Long problemId){
        Optional<ClimbingProblem> result = climbingProblemRepository.findById(problemId);
        if (result.isEmpty()){
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    String.format("Climbing problem with id %d is no longer exist.",
                            problemId));
        }
        ClimbingProblem problem = result.get();
        if (problem.getProblemStatus().equals(LifecycleStatus.ARCHIVE)){
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    String.format("Climbing problem %s is no longer active.", problem.getProblemInfo()));
        }
        return problem;
    }

    public List<ClimbingProblem> getAllActiveProblemFromWallSection(WallSection section){
        List<ClimbingProblem> problems = climbingProblemRepository.findByWallSection(section);
        return problems.stream()
                .filter(problem -> problem.getProblemStatus().equals(LifecycleStatus.ACTIVE))
                .toList();
    }

    public ClimbingProblem generateNewClimbingProblem(WallSection wall, ClimbingProblemCreationRequest request){
        ClimbingGrade assignedGrade = getClimbingGrade(request.assignedGrade());
        ClimbingProblem problem = new ClimbingProblem();
        problem.setHoldColor(request.holdColor());
        problem.setProblemInfo(request.info());
        problem.setProblemStatus(LifecycleStatus.ACTIVE);
        problem.setClimbingGrade(assignedGrade);
        problem.setCreatedDate(LocalDateTime.now());
        problem.setWallSection(wall);
        return climbingProblemRepository.save(problem);
    }

    private ClimbingGrade getClimbingGrade(GradeDefinition grade){
        Optional<ClimbingGrade> climbingGrade = climbingGradeRepository.findByGradeDefinition(grade);
        return climbingGrade.orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "The assigned grade %s is not a proper. Please contact the developers for support.",
                                grade.name()
                        )
                )
        );
    }

    public List<ClimbingProblem> getAllProblemsFromWallSection(WallSection section){
        return climbingProblemRepository.findByWallSection(section);
    }

    public void archiveActiveProblems(List<ClimbingProblem> problems){
        problems.forEach(p -> p.setProblemStatus(LifecycleStatus.ARCHIVE));
        climbingProblemRepository.saveAll(problems);
    }

    public void disconnectFromWallSection(List<ClimbingProblem> problems){
        problems.forEach(p -> {
            p.setWallSection(null);
            p.setProblemStatus(LifecycleStatus.ARCHIVE);
        });
        climbingProblemRepository.saveAll(problems);
    }
}
