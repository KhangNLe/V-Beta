package app.VBeta.application;

import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.climb.ClimbingGrade;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.application.support.grade.ClimbingGradeManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@code ProblemFilteringService} provides discovery queries for active climbing problems
 * within a wall section, filtered by an inclusive grade range.
 * <p>
 * It validates wall sections, resolves {@link GradeDefinition} values to persisted
 * {@link ClimbingGrade} rows through {@link ClimbingGradeManager}, and delegates
 * persistence queries to {@link ClimbingProblemManager}.
 */
@Service
@Transactional
public class ProblemFilteringService {
    private final WallSectionManager wallSectionManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final ClimbingGradeManager climbingGradeManager;

    /**
     * Constructs a new {@code ProblemFilteringService} with wall, problem, and grade dependencies.
     *
     * @param wallSectionManager manager for wall section lookups
     * @param climbingProblemManager manager for climbing problem queries
     * @param climbingGradeManager manager for climbing grade lookups
     */
    public  ProblemFilteringService(WallSectionManager wallSectionManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingGradeManager climbingGradeManager){
        this.climbingProblemManager = climbingProblemManager;
        this.wallSectionManager = wallSectionManager;
        this.climbingGradeManager = climbingGradeManager;
    }

    /**
     * Returns active problems in a wall section whose assigned grade falls within the
     * inclusive range [{@code lowestGrade}, {@code highestGrade}], unsorted.
     *
     * @param wallSectionId wall section identifier
     * @param lowestGrade inclusive lower bound grade definition
     * @param highestGrade inclusive upper bound grade definition
     * @return matching problems as API response DTOs
     * @throws RuntimeException when the wall section does not exist
     */
    public List<ClimbingProblemResponse> findProblemsByRange(Long wallSectionId, GradeDefinition lowestGrade,
                                                             GradeDefinition highestGrade){
        verifyGradeRange(lowestGrade, highestGrade);
        WallSection wall = getAndValidateWallSection(wallSectionId);
        ClimbingGrade minGrade = climbingGradeManager.getClimbingGradeByDefinition(lowestGrade);
        ClimbingGrade maxGrade = climbingGradeManager.getClimbingGradeByDefinition(highestGrade);

        List<ClimbingProblem> problems = climbingProblemManager.getActiveProblemBetweenGrade(wall, minGrade, maxGrade);
        return getProblemResponse(problems);
    }

    /**
     * Returns active problems in a wall section within the inclusive grade range,
     * ordered by assigned grade ascending (easier to harder).
     *
     * @param wallSectionId wall section identifier
     * @param lowestGrade inclusive lower bound grade definition
     * @param highestGrade inclusive upper bound grade definition
     * @return matching problems as API response DTOs, sorted ascending by grade
     * @throws RuntimeException when the wall section does not exist
     */
    public List<ClimbingProblemResponse> findProblemBetweenRangeAsc(Long wallSectionId, GradeDefinition lowestGrade,
                                                                    GradeDefinition highestGrade){
        verifyGradeRange(lowestGrade, highestGrade);
        WallSection wall = getAndValidateWallSection(wallSectionId);
        ClimbingGrade minGrade =  climbingGradeManager.getClimbingGradeByDefinition(lowestGrade);
        ClimbingGrade maxGrade = climbingGradeManager.getClimbingGradeByDefinition(highestGrade);
        List<ClimbingProblem> problems = climbingProblemManager.getActiveProblemBetweenAsc(wall, minGrade, maxGrade);

        return getProblemResponse(problems);
    }

    /**
     * Returns active problems in a wall section within the inclusive grade range,
     * ordered by assigned grade descending (harder to easier).
     *
     * @param wallSectionId wall section identifier
     * @param lowestGrade inclusive lower bound grade definition
     * @param highestGrade inclusive upper bound grade definition
     * @return matching problems as API response DTOs, sorted descending by grade
     * @throws RuntimeException when the wall section does not exist
     */
    public List<ClimbingProblemResponse> findProblemBetweenRangeDesc(Long wallSectionId, GradeDefinition lowestGrade,
                                                                     GradeDefinition highestGrade){
        verifyGradeRange(lowestGrade, highestGrade);
        WallSection wall = getAndValidateWallSection(wallSectionId);
        ClimbingGrade minGrade = climbingGradeManager.getClimbingGradeByDefinition(lowestGrade);
        ClimbingGrade maxGrade = climbingGradeManager.getClimbingGradeByDefinition(highestGrade);
        List<ClimbingProblem> problems = climbingProblemManager.getActiveProblemBetweenDesc(wall, minGrade, maxGrade);

        return getProblemResponse(problems);
    }

    /**
     * Loads a wall section by ID and fails when it cannot be found.
     *
     * @param wallSectionId wall section identifier
     * @return persisted wall section
     * @throws RuntimeException when the wall section is missing
     */
    private WallSection getAndValidateWallSection(Long wallSectionId){
        WallSection wall = wallSectionManager.findWallSection(wallSectionId);
        if (wall == null){
            throw new RuntimeException("Unable to find the wall section");
        }
        return wall;
    }

    /**
     * Maps climbing problem entities to API response DTOs.
     *
     * @param problems climbing problem entities
     * @return response DTOs for the provided problems
     */
    private List<ClimbingProblemResponse> getProblemResponse(List<ClimbingProblem> problems){
        return problems.stream().map(
                problem -> new ClimbingProblemResponse(
                        problem.getId(),
                        problem.getHoldColor(),
                        problem.getProblemInfo(),
                        problem.getCreatedDate().toString(),
                        problem.getClimbingGrade().getGradeDefinition(),
                        problem.getProblemImageUrl())
        ).toList();
    }

    private void verifyGradeRange(GradeDefinition lowerGrade,  GradeDefinition upperGrade){
        if (lowerGrade.compareTo(upperGrade) > 0){
            throw new RuntimeException("The lower grade range cannot be higher than the upper grade.");
        }

    }
}
