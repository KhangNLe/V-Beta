package app.VBeta.controller;

import app.VBeta.api.dto.discussions.*;
import app.VBeta.api.dto.discussions.comment.CommentDeletionRequest;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.discussions.video.SolutionBetaCreateRequest;
import app.VBeta.api.dto.discussions.video.SolutionBetaDeletionRequest;
import app.VBeta.api.dto.problems.ClimbingProblemDetailResponse;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.domain.model.actions.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * {@code ProblemDiscussionController} handles discussion interactions for climbing problems.
 * <p>
 * Endpoints include user comments, solution beta upload lifecycle, and perceived grade submissions.
 * Business logic is delegated to {@link ProblemDiscussionService} and {@link ClimbingWallService}.
 * Authorization checks are performed via {@link AuthorizationService} for privileged actions.
 */
@RestController
@RequestMapping("/api/discussion")
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
    public ResponseEntity<?> addUserComment(@Valid @RequestBody DiscussionCommentRequest request) {
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();

            UserDiscussionData response =  problemDiscussionService.addComment(firebaseUid, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates signed upload metadata for a solution beta video.
     *
     * @param body cloud file storage request payload
     * @return signed upload URL and related storage metadata
     */
    @GetMapping("/solution-beta/upload-url")
    public ResponseEntity<?> getSignedURL(@RequestBody CloudFileStorageRequest body){
        try {
            CloudFileStorageResponse response = problemDiscussionService.getSignedUrl(body);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Submits a user's perceived grade and returns updated problem details.
     *
     * @param problemId climbing problem identifier
     * @param request perceived grade request payload
     * @return updated climbing problem detail response
     */
    @PostMapping("/problems/{problemId}/suggest-grade")
    public ResponseEntity<?> givePerceiveGrade(@PathVariable Long problemId,
                                                           @Valid @RequestBody PerceiveGradeRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.GRADE_PROBLEM);
            problemDiscussionService.addClimbingProblemPerceiveGrade(firebaseUid, problemId, request);
            ClimbingProblemDetailResponse response = climbingWallService.getClimbingProblem(problemId);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Persists a solution beta entry after upload completion.
     *
     * @param request solution beta creation payload
     * @return discussion timeline item representing the uploaded beta
     */
    @PostMapping("solution-beta/save")
    public ResponseEntity<?> storeUserSolutionBeta(@Valid @RequestBody SolutionBetaCreateRequest request) {
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            UserDiscussionData response =  problemDiscussionService.saveSolutionBeta(request, firebaseUid);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return  new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a user solution beta entry.
     *
     * @param request solution beta deletion payload
     */
    @DeleteMapping("/solution-beta")
    public ResponseEntity<?> deleteUserSolutionBeta(@RequestBody SolutionBetaDeletionRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            problemDiscussionService.softDeleteUserSolutionBeta(request, firebaseUid);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    public ResponseEntity<?> deleteComment(@Valid @RequestBody CommentDeletionRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_COMMENT);
            problemDiscussionService.softDeleteUserComment(firebaseUid, request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
