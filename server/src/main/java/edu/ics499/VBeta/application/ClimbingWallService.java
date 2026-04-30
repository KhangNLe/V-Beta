package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.WallSection;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * {@code ClimbingWallService} coordinates business operations for wall sections and climbing problems.
 * It assembles API response DTOs, validates entity existence, and orchestrates lifecycle changes such as
 * creation, archive, and deletion.
 * <p>
 * Core operations are delegated to support managers including {@link WallSectionManager},
 * {@link ClimbingProblemManager}, and {@link ClimbingProblemDeletionManager}.
 */
@Service
@Transactional
public class ClimbingWallService {
    private static final String WALL_SECTIONS_CACHE = "wallSections";
    private static final String CLIMBING_PROBLEMS_CACHE = "climbingProblems";

    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final WallSectionManager wallSectionManager;
    private final ClimbingProblemDeletionManager climbingProblemDeletionManager;
    private final UserPerceiveGradeManager userPerceiveGradeManager;

    /**
     * Constructs a new {@code ClimbingWallService} with required domain managers.
     *
     * @param climbingProblemDiscussionManager manager for discussion timeline composition
     * @param climbingProblemManager manager for climbing problem persistence and state
     * @param wallSectionManager manager for wall section operations
     * @param climbingProblemDeletionManager manager for cascading problem deletion
     * @param userPerceiveGradeManager manager for perceived grade aggregation
     */
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

    /**
     * Returns all wall sections in lightweight response form.
     *
     * @return list of wall section responses
     */
    @Cacheable(WALL_SECTIONS_CACHE)
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

    /**
     * Returns detailed climbing problem data, including discussion and perceived grade.
     *
     * @param problemId climbing problem identifier
     * @return detailed climbing problem response
     */
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

    /**
     * Returns active climbing problems under a wall section.
     *
     * @param wallSectionId wall section identifier
     * @return list of climbing problem summaries
     */
    @Cacheable(value = CLIMBING_PROBLEMS_CACHE, key = "#wallSectionId")
    public List<ClimbingProblemResponse> getClimbingProblemsByWallSectionId(Long wallSectionId) {
        WallSection wallSection = wallSectionManager.findWallSection(wallSectionId);
        return mapProblemsForWall(wallSection);
    }

    /**
     * Creates a new wall section.
     *
     * @param request wall section creation payload
     * @return created wall section response
     */
    @CacheEvict(value = WALL_SECTIONS_CACHE, allEntries = true)
    public WallSectionResponse createNewWallSection(WallSectionCreationRequest request){
        WallSection newWall = wallSectionManager.createNewWallSection(request);
        return new WallSectionResponse(
                newWall.getId(),
                newWall.getWallSectionName(),
                newWall.getWallInfo()
        );
    }

    /**
     * Deletes a wall section and all related climbing problem data.
     *
     * @param wallSectionId wall section identifier
     */
    @Caching(evict = {
            @CacheEvict(value = WALL_SECTIONS_CACHE, allEntries = true),
            @CacheEvict(value = CLIMBING_PROBLEMS_CACHE, key = "#wallSectionId")
    })
    public void deleteWallSection(Long wallSectionId){
        WallSection wallSection = wallSectionManager.findWallSection(wallSectionId);
        if (wallSection == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Incorrect wall section ID or the wall section does not exist.");
        }

        List<ClimbingProblem> problems = climbingProblemManager.getAllProblemsFromWallSection(wallSection);
        problems.forEach(climbingProblemDeletionManager::deleteClimbingProblem);
        wallSectionManager.removeWallSection(wallSectionId);
    }

    /**
     * Archives active problems in a wall section without deleting the section itself.
     *
     * @param wallSectionId wall section identifier
     */
    @CacheEvict(value = CLIMBING_PROBLEMS_CACHE, key = "#wallSectionId")
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
                        problem.getClimbingGrade().getGradeDefinition()
                ));
        });
        return problemsInfo;
    }

    /**
     * Creates a new climbing problem in a given wall section.
     *
     * @param wallSectionId wall section identifier
     * @param request climbing problem creation payload
     * @return created climbing problem response
     */
    @CacheEvict(value = CLIMBING_PROBLEMS_CACHE, key = "#wallSectionId")
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

    /**
     * Deletes a climbing problem and dependent discussion/beta data.
     *
     * @param problemId climbing problem identifier
     */
    @CacheEvict(value = CLIMBING_PROBLEMS_CACHE, allEntries = true)
    public void deleteClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        climbingProblemDeletionManager.deleteClimbingProblem(problem);
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
