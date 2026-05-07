package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ClimbingWallService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * {@code ProblemDiscussionController} handles discussion interactions for climbing problems.
 * <p>
 * Endpoints include user comments, solution beta upload lifecycle, and perceived grade submissions.
 * Business logic is delegated to {@link ProblemDiscussionService} and {@link ClimbingWallService}.
 * Authorization checks are performed via {@link AuthorizationService} for privileged actions.
 */
@RestController
@RequestMapping("/discussion")
public class ProblemDiscussionController {
    private final ProblemDiscussionService problemDiscussionService;
    private final AuthorizationService authorizationService;
    private final ClimbingWallService climbingWallService;

    /**
     * Constructs a new {@code ProblemDiscussionController} with required services.
     *
     * @param problemDiscussionService service for discussion and solution-beta operations
     * @param authorizationService service for authentication/authorization checks
     * @param climbingWallService service for returning updated problem details
     */
    public ProblemDiscussionController(ProblemDiscussionService problemDiscussionService,
                                       AuthorizationService authorizationService,
                                       ClimbingWallService climbingWallService){
        this.problemDiscussionService = problemDiscussionService;
        this.authorizationService = authorizationService;
        this.climbingWallService = climbingWallService;
    }

    /**
     * Adds a text comment to a climbing problem discussion.
     *
     * @param request discussion comment payload
     * @return created discussion timeline entry, including {@code discussionId}
     */
    @PostMapping("/add-comments")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCommentData addUserComment(@Valid @RequestBody DiscussionCommentRequest request) {
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();

        return problemDiscussionService.addComment(firebaseUid, request);
    }

    /**
     * Creates signed upload metadata for a solution beta video.
     *
     * @param body cloud file storage request payload
     * @return signed upload URL and related storage metadata
     */
    @PostMapping("/solution-beta/upload-url")
    public CloudFileStorageResponse getSignedURL(@RequestBody CloudFileStorageRequest body){
        return problemDiscussionService.getSignedUrl(body);
    }

    /**
     * Submits a user's perceived grade and returns updated problem details.
     *
     * @param problemId climbing problem identifier
     * @param request perceived grade request payload
     * @return updated climbing problem detail response
     */
    @PostMapping("/problems/{problemId}/suggest-grade")
    public ClimbingProblemDetailResponse givePerceiveGrade(@PathVariable Long problemId,
                                                           @Valid @RequestBody PerceiveGradeRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.GRADE_PROBLEM);
        problemDiscussionService.addClimbingProblemPerceiveGrade(firebaseUid, problemId, request);
        return climbingWallService.getClimbingProblem(problemId);
    }

    /**
     * Persists a solution beta entry after upload completion.
     *
     * @param request solution beta creation payload
     * @return discussion timeline item representing the uploaded beta
     */
    @PostMapping("solution-beta/save")
    public UserCommentData storeUserSolutionBeta(@Valid @RequestBody SolutionBetaCreateRequest request) {
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        return problemDiscussionService.saveSolutionBeta(request, firebaseUid);
    }

    /**
     * Deletes a user solution beta entry.
     *
     * @param request solution beta deletion payload
     */
    @DeleteMapping("/solution-beta")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUserSolutionBeta(@RequestBody SolutionBetaDeletionRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        problemDiscussionService.removeUserSolutionBeta(request, firebaseUid);
    }

    /**
     * Deletes a discussion comment authored by a user.
     * <p>
     * Caller must be authorized for {@link ActionDefinition#DELETE_COMMENT}. The underlying
     * service validates whether the requester is either the author or an administrator.
     *
     * @param request comment deletion payload containing author/problem/content identifiers
     */
    @DeleteMapping("/comment/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteComment(@Valid @RequestBody CommentDeletionRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_COMMENT);
        problemDiscussionService.removeUserComment(firebaseUid, request);
    }
}
