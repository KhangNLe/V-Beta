package app.VBeta.controller;

import app.VBeta.api.dto.ClimbingProblemResponse;
import app.VBeta.application.ProblemFilteringService;
import app.VBeta.domain.model.ClimbingProblem;
import app.VBeta.domain.model.GradeDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * {@code ProblemDiscoveryController} exposes search and grade-range discovery endpoints
 * under {@code /search}.
 * <p>
 * It delegates filtering and sorting to {@link ProblemFilteringService} and returns
 * active climbing problems as {@link ClimbingProblemResponse} lists.
 */
@RestController
@RequestMapping("/search")
public class ProblemDiscoveryController {
    private final ProblemFilteringService problemFilteringService;

    /**
     * Constructs a new {@code ProblemDiscoveryController} with filtering service dependency.
     *
     * @param problemFilteringService service for grade-range problem discovery
     */
    public ProblemDiscoveryController(ProblemFilteringService problemFilteringService){
        this.problemFilteringService = problemFilteringService;
    }

    /**
     * Returns active problems in a wall section within an inclusive grade range, unsorted.
     *
     * @param wallSectionId wall section identifier
     * @param lowestGrade inclusive lower bound grade
     * @param highestGrade inclusive upper bound grade
     * @return {@code 200 OK} with matching problem responses
     */
    @GetMapping("/{wallSectionId}/range={lowestGrade}-{highestGrade}")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeRange(@PathVariable Long wallSectionId,
                                                                                   @PathVariable GradeDefinition lowestGrade,
                                                                                   @PathVariable GradeDefinition highestGrade) {
        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(wallSectionId,
                lowestGrade, highestGrade);

        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    /**
     * Returns active problems in a wall section within an inclusive grade range,
     * sorted by assigned grade ascending.
     *
     * @param wallSectionId wall section identifier
     * @param lowest inclusive lower bound grade
     * @param highest inclusive upper bound grade
     * @return {@code 200 OK} with matching problem responses ordered ascending by grade
     */
    @GetMapping("/{wallSectionId}/range={lowest}-{highest}&sort=asc")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeAsc(@PathVariable Long wallSectionId,
                                                                                 @PathVariable GradeDefinition lowest,
                                                                                 @PathVariable GradeDefinition highest) {
        List<ClimbingProblemResponse> problemResponses = problemFilteringService.findProblemBetweenRangeAsc(
                wallSectionId, lowest, highest);

        return new ResponseEntity<>(problemResponses, HttpStatus.OK);
    }

    /**
     * Returns active problems in a wall section within an inclusive grade range,
     * sorted by assigned grade descending.
     *
     * @param wallSectionId wall section identifier
     * @param lowest inclusive lower bound grade
     * @param highest inclusive upper bound grade
     * @return {@code 200 OK} with matching problem responses ordered descending by grade
     */
    @GetMapping("/{wallSectionId}/range={lowest}-{highest}&sort=desc")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeDesc(@PathVariable Long wallSectionId,
                                                                                  @PathVariable GradeDefinition lowest,
                                                                                  @PathVariable GradeDefinition highest){
        List<ClimbingProblemResponse> problemResponses = problemFilteringService.findProblemBetweenRangeDesc(
                wallSectionId, lowest, highest);

        return new  ResponseEntity<>(problemResponses, HttpStatus.OK);
    }

}
