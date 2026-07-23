package app.VBeta.controller;

import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.application.ProblemFilteringService;
import app.VBeta.domain.model.climb.GradeDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code ProblemDiscoveryController} exposes grade-range discovery endpoints under {@code /search}.
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
     * Returns active problems in a wall section within an inclusive grade range.
     * <p>
     * Optional {@code sort} values: {@code asc}, {@code desc}. When omitted, results are unsorted.
     *
     * @param wallSectionId wall section identifier
     * @param min inclusive lower bound grade
     * @param max inclusive upper bound grade
     * @param sort optional sort direction ({@code asc} or {@code desc})
     * @return {@code 200 OK} with matching problem responses
     */
    @GetMapping("/{wallSectionId}")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeRange(
            @PathVariable Long wallSectionId,
            @RequestParam GradeDefinition min,
            @RequestParam GradeDefinition max,
            @RequestParam(required = false) String sort) {
        List<ClimbingProblemResponse> responses;
        if ("asc".equalsIgnoreCase(sort)) {
            responses = problemFilteringService.findProblemBetweenRangeAsc(wallSectionId, min, max);
        } else if ("desc".equalsIgnoreCase(sort)) {
            responses = problemFilteringService.findProblemBetweenRangeDesc(wallSectionId, min, max);
        } else {
            responses = problemFilteringService.findProblemsByRange(wallSectionId, min, max);
        }
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}
