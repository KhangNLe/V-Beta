package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.api.dto.WallSectionResponse;
import edu.ics499.VBeta.api.dto.ClimbingProblemDetailResponse;
import edu.ics499.VBeta.application.WallSectionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home")
public class WallSectionController {
    private final WallSectionManager wallSectionManager;

    public WallSectionController(WallSectionManager wallSectionManager){
        this.wallSectionManager = wallSectionManager;
    }

    @GetMapping("/wall-sections")
    @Transactional(readOnly = true)
    public List<WallSectionResponse> wallSections() {
        return wallSectionManager.getWallSections();
    }

    @GetMapping("/wall-sections/{wallSectionId}/problems")
    @Transactional(readOnly = true)
    public List<ClimbingProblemResponse> problemsForWallSection(@PathVariable Long wallSectionId) {
        return wallSectionManager.getClimbingProblemsByWallSectionId(wallSectionId);
    }

    @GetMapping("wall-sections/{wallSectionId}/problems/{problemID}")
    @Transactional(readOnly = true)
    public ClimbingProblemDetailResponse getProblemDetail(@PathVariable Long wallSectionId, @PathVariable Long problemID){
        return wallSectionManager.getClimbingProblem(problemID);
    }

}