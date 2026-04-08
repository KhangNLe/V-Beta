package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.application.SolutionBetaManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discussion")
public class ProblemDiscussionController {
    private final SolutionBetaManager solutionBetaManager;

    public ProblemDiscussionController(SolutionBetaManager solutionBetaManager){
        this.solutionBetaManager = solutionBetaManager;
    }

    @PostMapping("/solution-beta/upload-url")
    public CloudFileStorageResponse getSignedURL(@RequestBody CloudFileStorageRequest body){
        return solutionBetaManager.createSignedUrl(body);
    }
}
