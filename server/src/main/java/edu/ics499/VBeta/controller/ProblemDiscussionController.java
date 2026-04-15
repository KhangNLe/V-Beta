package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.CommentDeletionRequest;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.application.support.SolutionBetaManager;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
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

    @DeleteMapping("/comment/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteComment(@Valid @RequestBody CommentDeletionRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_COMMENT);
        problemDiscussionService.removeUserComment(firebaseUid, request);
    }
}
