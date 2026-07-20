package app.VBeta.controller;

import app.VBeta.api.dto.ClimbingProblemResponse;
import app.VBeta.application.ProblemFilteringService;
import app.VBeta.domain.model.GradeDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/search")
public class ProblemDiscoveryController {
    private final ProblemFilteringService problemFilteringService;

    public ProblemDiscoveryController(ProblemFilteringService problemFilteringService){
        this.problemFilteringService = problemFilteringService;
    }

    @GetMapping("/{wallSectionId}/{grade}")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGrade(@PathVariable Long wallSectionId,
                                                                              @PathVariable GradeDefinition grade){
        return null;
    }

    @GetMapping("/{wallSectionId}/asc")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeAsc(@PathVariable Long wallSectionId){
        return null;
    }

    @GetMapping("/{wallSectionId}/desc")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeDesc(@PathVariable Long wallSectionId){
        return null;
    }

    @GetMapping("/{wallSectionId}/range/{lowestGrade}/{highestGrade}")
    public ResponseEntity<List<ClimbingProblemResponse>> filterProblemByGradeRange(@PathVariable Long wallSectionId,
                                                                   @PathVariable GradeDefinition lowestGrade,
                                                                   @PathVariable GradeDefinition highestGrade){
        return null;
    }
}
