package app.VBeta.application.support.problem;

import app.VBeta.api.dto.ClimbingProblemCreationRequest;
import app.VBeta.domain.model.*;
import app.VBeta.repository.ClimbingGradeRepository;
import app.VBeta.repository.ClimbingProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code ClimbingProblemManager} encapsulates persistence and lifecycle rules for
 * {@link ClimbingProblem} entities.
 * <p>
 * It provides active/archive filtering, validates assigned grades through
 * {@link ClimbingGradeRepository}, and creates new problems from
 * {@link ClimbingProblemCreationRequest} payloads.
 */
@Service
@Transactional
public class ClimbingProblemManager {
    private final ClimbingProblemRepository climbingProblemRepository;
    private final ClimbingGradeRepository climbingGradeRepository;

    /**
     * Constructs a new {@code ClimbingProblemManager} with problem and grade repositories.
     *
     * @param climbingProblemRepository repository for climbing problem entities
     * @param climbingGradeRepository repository for grade definition lookups
     */
    public ClimbingProblemManager(ClimbingProblemRepository climbingProblemRepository,
                                  ClimbingGradeRepository climbingGradeRepository){
        this.climbingProblemRepository = climbingProblemRepository;
        this.climbingGradeRepository = climbingGradeRepository;
    }

    /**
     * Returns an active climbing problem by ID.
     *
     * @param problemId climbing problem identifier
     * @return active climbing problem or {@code null} when missing/archived
     */
    public ClimbingProblem getActiveProblem(Long problemId){
        Optional<ClimbingProblem> result = climbingProblemRepository.findById(problemId);
        if (result.isEmpty() || result.get().getProblemStatus().equals(LifecycleStatus.ARCHIVE)){
            return null;
        }
        return result.get();
    }

    /**
     * Returns active problems assigned to a wall section.
     *
     * @param section wall section context
     * @return active climbing problems in the section
     */
    public List<ClimbingProblem> getAllActiveProblemFromWallSection(WallSection section){
        List<ClimbingProblem> problems = climbingProblemRepository.findByWallSection(section);
        return problems.stream()
                .filter(problem -> problem.getProblemStatus().equals(LifecycleStatus.ACTIVE))
                .toList();
    }

    /**
     * Creates and persists a new active climbing problem.
     *
     * @param wall target wall section
     * @param request climbing problem creation payload
     * @return persisted climbing problem
     */
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

    /**
     * Returns all problems in a wall section regardless of lifecycle state.
     *
     * @param section wall section context
     * @return all climbing problems in the section
     */
    public List<ClimbingProblem> getAllProblemsFromWallSection(WallSection section){
        return climbingProblemRepository.findByWallSection(section);
    }

    /**
     * Archives each provided active climbing problem.
     *
     * @param problems problems to archive
     */
    public void archiveActiveProblems(List<ClimbingProblem> problems){
        problems.forEach(p -> p.setProblemStatus(LifecycleStatus.ARCHIVE));
        climbingProblemRepository.saveAll(problems);
    }

    /**
     * Returns active problems in a wall section whose assigned grade is inclusively
     * between {@code minGrade} and {@code maxGrade}, unsorted.
     *
     * @param wall wall section context
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching active climbing problems
     */
    public List<ClimbingProblem> getActiveProblemBetweenGrade(WallSection wall, ClimbingGrade minGrade,
                                                           ClimbingGrade maxGrade){
        return climbingProblemRepository.findByWallSectionAndProblemStatusAndClimbingGradeBetween(
                wall, LifecycleStatus.ACTIVE, minGrade, maxGrade
        );
    }

    /**
     * Returns active problems in a wall section within an inclusive grade range,
     * ordered by assigned grade ascending.
     *
     * @param wall wall section context
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching active climbing problems sorted ascending by grade
     */
    public List<ClimbingProblem> getActiveProblemBetweenAsc(WallSection wall, ClimbingGrade minGrade,
                                                            ClimbingGrade maxGrade){
        return climbingProblemRepository.findByWallSectionAndProblemStatusAndClimbingGradeBetweenOrderByClimbingGradeAsc
                (wall, LifecycleStatus.ACTIVE, minGrade, maxGrade);
    }

    /**
     * Returns active problems in a wall section within an inclusive grade range,
     * ordered by assigned grade descending.
     *
     * @param wall wall section context
     * @param minGrade inclusive lower bound grade
     * @param maxGrade inclusive upper bound grade
     * @return matching active climbing problems sorted descending by grade
     */
    public List<ClimbingProblem> getActiveProblemBetweenDesc(WallSection wall, ClimbingGrade minGrade,
                                                             ClimbingGrade maxGrade){
        return climbingProblemRepository.findByWallSectionAndProblemStatusAndClimbingGradeBetweenOrderByClimbingGradeDesc
                (wall, LifecycleStatus.ACTIVE, minGrade, maxGrade);
    }
}
