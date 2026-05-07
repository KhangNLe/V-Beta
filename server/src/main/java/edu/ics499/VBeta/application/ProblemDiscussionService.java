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
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.application.support.UserAccountManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * {@code ProblemDiscussionService} is the orchestration layer for discussion-thread interactions
 * around climbing problems, including text comments, beta video uploads, and perceived grade updates.
 * <p>
 * It validates user/problem context and delegates persistence operations to
 * specialized managers, while enforcing discussion-id-based authorization checks
 * for deletion operations.
 */
@Service
@Transactional
public class ProblemDiscussionService {
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final SolutionBetaManager solutionBetaManager;
    private final UserPerceiveGradeManager userPerceiveGradeManager;
    private final DiscussionRootManager discussionRootManager;

    /**
     * Constructs a new {@code ProblemDiscussionService} with required collaborators.
     *
     * @param userAccountManager manager for account lookups
     * @param climbingProblemManager manager for climbing problem retrieval
     * @param climbingProblemDiscussionManager manager for discussion comment persistence
     * @param solutionBetaManager manager for beta storage and uploads
     * @param userPerceiveGradeManager manager for perceived grade writes
     * @param discussionRootManager manager for discussion-root authorization lookups
     */
    public ProblemDiscussionService(UserAccountManager userAccountManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
                                    SolutionBetaManager solutionBetaManager,
                                    UserPerceiveGradeManager userPerceiveGradeManager, DiscussionRootManager discussionRootManager){
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.solutionBetaManager = solutionBetaManager;
        this.userPerceiveGradeManager = userPerceiveGradeManager;
        this.discussionRootManager = discussionRootManager;
    }

    /**
     * Adds a discussion comment for an active climbing problem.
     *
     * @param firebaseUid Firebase UID of the authenticated user
     * @param request discussion comment payload
     * @return created discussion entry including {@code discussionId} for later operations
     */
    public UserCommentData addComment(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        return climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
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
     * @return discussion timeline entry for the uploaded beta, including {@code discussionId}
     */
    public UserCommentData saveSolutionBeta(SolutionBetaCreateRequest request, String firebaseUid){
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        SolutionBeta solutionBeta = climbingProblemDiscussionManager.storeSolutionBeta(userAccount, problem,
                request.objectFileName(), request.videoURL());
        return new UserCommentData(
                solutionBeta.getDiscussionRoot().getDiscussionId(),
                userAccount.getId(),
                userAccount.getUsername(),
                null,
                DiscussionType.BETA,
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
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        validateDeletionOwnerObject(requestUser, request.userId());
        validateDiscussionExisting(request.discussionId(), requestUser, problem);
        climbingProblemDiscussionManager.removeUserSolutionBeta(request.discussionId(), request.publicUrl());
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

    /**
     * Removes a user discussion comment when requester is the author or an admin.
     *
     * @param firebaseUid Firebase UID of the authenticated requester
     * @param request comment deletion payload
     */
    public void removeUserComment(String firebaseUid, CommentDeletionRequest request){
        UserAccount requestUser = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());

        validateDeletionOwnerObject(requestUser, request.authorId());
        validateDiscussionExisting(request.discussionId(), requestUser, problem);


        climbingProblemDiscussionManager.removeUserComment(request.discussionId(), request.commentContent());
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

    /**
     * Validates that deletion is requested by the original author or an administrator.
     *
     * @param user requester account
     * @param authorId author user ID associated with the target content
     * @throws ResponseStatusException with {@link HttpStatus#UNAUTHORIZED} when deletion is not permitted
     */
    private void validateDeletionOwnerObject(UserAccount user, Long authorId){
        if (!Objects.equals(user.getId(), authorId) &&
                !user.getGymRole().getRoleType().equals(RoleType.ADMIN)){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Action. Cannot remove object from different author"
            );
        }
    }

    /**
     * Validates that a discussion id belongs to the requester on the given problem,
     * unless requester is an administrator.
     *
     * @param requestDiscussionId discussion id to validate
     * @param requestUser requester account
     * @param problem climbing problem context
     * @throws ResponseStatusException with {@link HttpStatus#UNAUTHORIZED} when requester
     * does not own the discussion and is not admin
     */
    private void validateDiscussionExisting(Long requestDiscussionId, UserAccount requestUser,
                                            ClimbingProblem problem){
        List<DiscussionRoot> discussionRoots = discussionRootManager.findDiscussionRootByUserAndProblem(requestUser,
                problem);

        boolean isExist = discussionRoots.stream()
                .anyMatch(d -> d.getDiscussionId().equals(requestDiscussionId));

        if  (!isExist && !requestUser.getGymRole().getRoleType().equals(RoleType.ADMIN)){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid action. Cannot remove object from different author"
            );
        }
    }
}
