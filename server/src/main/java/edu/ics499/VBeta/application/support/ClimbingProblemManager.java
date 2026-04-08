package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class ClimbingProblemManager {
    private final ClimbingProblemRepository climbingProblemRepository;

    public ClimbingProblemManager(ClimbingProblemRepository climbingProblemRepository){
        this.climbingProblemRepository = climbingProblemRepository;
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
}
