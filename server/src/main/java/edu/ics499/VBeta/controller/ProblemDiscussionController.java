package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.application.support.SolutionBetaManager;
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

    public ProblemDiscussionController(ProblemDiscussionService problemDiscussionService,
                                       AuthorizationService authorizationService){
        this.problemDiscussionService = problemDiscussionService;
        this.authorizationService = authorizationService;
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

    @PostMapping("solution-beta/save")
    public UserCommentData storeUserSolutionBeta(@Valid @RequestBody SolutionBetaCreateRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        return problemDiscussionService.saveSolutionBeta(request, firebaseUid);
    }

    @PostMapping("/solution-beta/{problemId}/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUserSolutionBeta(@RequestBody UserCommentData requestData, @Valid @PathVariable Long problemId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        problemDiscussionService.removeUserSolutionBeta(requestData, firebaseUid, problemId);
    }
}
