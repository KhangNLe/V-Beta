package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ClimbingWallService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.application.support.SolutionBetaManager;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/discussion")
public class ProblemDiscussionController {
    private final ProblemDiscussionService problemDiscussionService;
    private final AuthorizationService authorizationService;
    private final ClimbingWallService climbingWallService;

    public ProblemDiscussionController(ProblemDiscussionService problemDiscussionService,
                                       AuthorizationService authorizationService,
                                       ClimbingWallService climbingWallService){
        this.problemDiscussionService = problemDiscussionService;
        this.authorizationService = authorizationService;
        this.climbingWallService = climbingWallService;
    }

    @PostMapping("/add-comments")
    @ResponseStatus(HttpStatus.CREATED)
    public void addUserComment(@Valid @RequestBody DiscussionCommentRequest request) {
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();

        problemDiscussionService.addComment(firebaseUid, request);
    }

    @PostMapping("/solution-beta/upload-url")
    public CloudFileStorageResponse getSignedURL(@RequestBody CloudFileStorageRequest body){
        return problemDiscussionService.getSignedUrl(body);
    }

    @PostMapping("/problems/{problemId}/suggest-grade")
    public ClimbingProblemDetailResponse givePerceiveGrade(@PathVariable Long problemId,
                                                           @Valid @RequestBody PerceiveGradeRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.GRADE_PROBLEM);
        problemDiscussionService.addClimbingProblemPerceiveGrade(firebaseUid, problemId, request);
        return climbingWallService.getClimbingProblem(problemId);
    }}
