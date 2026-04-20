package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.api.dto.PerceiveGradeRequest;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.SolutionBetaManager;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.SolutionBeta;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.application.support.UserAccountManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * {@code ProblemDiscussionService} is the orchestration layer for discussion-thread interactions
 * around climbing problems, including text comments, beta video uploads, and perceived grade updates.
 * <p>
 * It validates user/problem context and delegates persistence operations to specialized managers such as
 * {@link ClimbingProblemDiscussionManager}, {@link SolutionBetaManager}, and
 * {@link UserPerceiveGradeManager}.
 */
@Service
@Transactional
public class ProblemDiscussionService {
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final SolutionBetaManager solutionBetaManager;
    private final UserPerceiveGradeManager userPerceiveGradeManager;

    /**
     * Constructs a new {@code ProblemDiscussionService} with required collaborators.
     *
     * @param userAccountManager manager for account lookups
     * @param climbingProblemManager manager for climbing problem retrieval
     * @param climbingProblemDiscussionManager manager for discussion comment persistence
     * @param solutionBetaManager manager for beta storage and uploads
     * @param userPerceiveGradeManager manager for perceived grade writes
     */
    public ProblemDiscussionService(UserAccountManager userAccountManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
                                    SolutionBetaManager solutionBetaManager,
                                    UserPerceiveGradeManager userPerceiveGradeManager){
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.solutionBetaManager = solutionBetaManager;
        this.userPerceiveGradeManager = userPerceiveGradeManager;
    }

    /**
     * Adds a discussion comment for an active climbing problem.
     *
     * @param firebaseUid Firebase UID of the authenticated user
     * @param request discussion comment payload
     */
    public void addComment(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
    }

    /**
     * Generates cloud upload metadata and signed URL for solution video upload.
     *
     * @param request cloud storage request payload
     * @return signed upload and public URL metadata
     */
    public CloudFileStorageResponse getSignedUrl(CloudFileStorageRequest request){
        return solutionBetaManager.createSignedUrl(request);
    }

    /**
     * Persists a user-submitted solution beta after upload completes.
     *
     * @param request solution beta creation payload
     * @param firebaseUid Firebase UID of the authenticated user
     * @return comment stream entry representing the uploaded video
     */
    public UserCommentData saveSolutionBeta(SolutionBetaCreateRequest request, String firebaseUid){
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        SolutionBeta solutionBeta = solutionBetaManager.storeUserSolutionBeta(userAccount, problem,
                request.objectFileName(), request.videoURL());
        return new UserCommentData(
                userAccount.getId(),
                userAccount.getUsername(),
                null,
                solutionBeta.getVideoURL(),
                solutionBeta.getCreateDate()
        );
    }

    /**
     * Removes a user's solution beta when requester is owner or admin.
     *
     * @param request solution beta deletion payload
     * @param firebaseUid Firebase UID of the authenticated requester
     */
    public void removeUserSolutionBeta(SolutionBetaDeletionRequest request, String firebaseUid){
        UserAccount requestUser = getUserAccount(firebaseUid);
        UserAccount solutionBetaOwner = getUserAccount(request.userId());
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        validateDeletionOwnerObject(requestUser, request.userId());
        solutionBetaManager.removeUserSolutionBeta(solutionBetaOwner, problem, request.publicUrl());
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount account =  userAccountManager.findUserAccount(firebaseUid);
        if (account == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User Account with the unique firebase ID does not exist. Please log in and try again.");
        }
        return account;
    }

    /**
     * Stores or updates the authenticated user's perceived grade for a problem.
     *
     * @param firebaseUid Firebase UID of the authenticated user
     * @param problemId climbing problem identifier
     * @param request perceived grade payload
     */
    public void addClimbingProblemPerceiveGrade(String firebaseUid, Long problemId, PerceiveGradeRequest request){
        ClimbingProblem problem = getActiveClimbingProblem(problemId);
        userPerceiveGradeManager.addPerceiveGrade(problem, firebaseUid, request.perceiveGrade());
    }

    private UserAccount getUserAccount(Long userId){
        UserAccount account =  userAccountManager.findUserAccountById(userId);
        if (account == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User Account with the unique firebase ID does not exist. Please log in and try again.");
        }
        return account;
    }

    private ClimbingProblem getActiveClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("The problem with ID %d does not exist or no longer active.", problemId));
        }
        return problem;
    }

    private void validateDeletionOwnerObject(UserAccount user, Long authorId){
        // Can only delete the object if it is from author or admin account
        if (!Objects.equals(user.getId(), authorId) &&
                !user.getGymRole().getRoleType().equals(RoleType.ADMIN)){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Action. Cannot remove object from different author"
            );
        }
    }
}
