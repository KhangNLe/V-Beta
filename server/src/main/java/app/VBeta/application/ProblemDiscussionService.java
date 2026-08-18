package app.VBeta.application;

import app.VBeta.api.dto.discussions.*;
import app.VBeta.api.dto.discussions.comment.CommentDeletionRequest;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.discussions.video.SolutionBetaCreateRequest;
import app.VBeta.api.dto.discussions.video.SolutionBetaDeletionRequest;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.grade.UserPerceiveGradeManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * {@code ProblemDiscussionService} is the orchestration layer for discussion-thread interactions
 * around climbing problems, including text comments, beta video uploads, and perceived grade updates.
 * <p>
 * It validates user/problem context and delegates persistence operations to
 * specialized managers, while enforcing discussion-id-based authorization checks
 * for soft-deletion operations.
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
    public UserDiscussionData addComment(String firebaseUid, DiscussionCommentRequest request){
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
    public UserDiscussionData saveSolutionBeta(SolutionBetaCreateRequest request, String firebaseUid){
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        SolutionBeta solutionBeta = climbingProblemDiscussionManager.storeSolutionBeta(userAccount, problem,
                request.objectFileName(), request.videoURL());
        return new UserDiscussionData(
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
     * Soft-deletes a user's solution beta when requester is owner or admin.
     * <p>
     * Sets {@code deleted_at}, {@code deleted_by}, and {@code deleted_reason} on the
     * discussion root. Child beta metadata and GCS objects are left in place.
     *
     * @param request solution beta deletion payload
     * @param firebaseUid Firebase UID of the authenticated requester
     */
    public void softDeleteUserSolutionBeta(SolutionBetaDeletionRequest request, String firebaseUid){
        UserAccount requestUser = getUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        validateDeletionOwnerObject(requestUser, request.userId());
        validateDiscussionExisting(request.discussionId(), requestUser, problem,
                new DiscussionContent(null, request.publicUrl()));

        climbingProblemDiscussionManager.softDeleteDiscussionRoot(requestUser, request.discussionId(),
                request.deleteReason());
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount account =  userAccountManager.findUserAccount(firebaseUid);
        if (account == null){
            throw new RuntimeException("User Account with the unique firebase ID does not exist. Please log in and try again.");
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
     * Soft-deletes a user discussion comment when requester is the author or an admin.
     * <p>
     * Sets {@code deleted_at}, {@code deleted_by}, and {@code deleted_reason} on the
     * discussion root. The comment child row is left in place.
     *
     * @param firebaseUid Firebase UID of the authenticated requester
     * @param request comment deletion payload
     */
    public void softDeleteUserComment(String firebaseUid, CommentDeletionRequest request){
        UserAccount requestUser = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());

        validateDeletionOwnerObject(requestUser, request.authorId());
        validateDiscussionExisting(request.discussionId(), requestUser, problem,
                new DiscussionContent(request.commentContent(), null));

        climbingProblemDiscussionManager.softDeleteDiscussionRoot(requestUser, request.discussionId(),
                request.deletedReason());
    }

    private UserAccount getUserAccount(Long userId){
        UserAccount account =  userAccountManager.findUserAccountById(userId);
        if (account == null){
            throw new RuntimeException("User Account with the unique firebase ID does not exist. Please log in and try again.");
        }
        return account;
    }

    private ClimbingProblem getActiveClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new RuntimeException(String.format("The problem with ID %d does not exist or no longer active.", problemId));
        }
        return problem;
    }

    /**
     * Validates that deletion is requested by the original author or an administrator.
     *
     * @param user requester account
     * @param authorId author user ID associated with the target content
     * @throws RuntimeException when deletion is not permitted
     */
    private void validateDeletionOwnerObject(UserAccount user, Long authorId){
        if (!Objects.equals(user.getId(), authorId) &&
                !user.getGymRole().getRoleType().equals(RoleType.ADMIN)){
            throw new RuntimeException("Invalid Action. Cannot remove object from different author");
        }
    }

    private record DiscussionContent(String commentContent, String betaUrl) {}

    /**
     * Validates that a discussion id exists on the given problem with matching content.
     * Non-admins must own the discussion; admins may target any discussion on the problem.
     *
     * @param requestDiscussionId discussion id to validate
     * @param requestUser requester account
     * @param problem climbing problem context
     * @param discussionContent expected comment text or beta URL
     * @throws RuntimeException when the discussion is missing, content mismatches,
     *         or a non-admin does not own the discussion
     */
    private void validateDiscussionExisting(Long requestDiscussionId, UserAccount requestUser,
                                            ClimbingProblem problem, DiscussionContent discussionContent){
        DiscussionRoot discussion;
        if (requestUser.getGymRole().getRoleType().equals(RoleType.ADMIN)) {
            discussion = discussionRootManager.findDiscussionRootById(requestDiscussionId);
            if (discussion == null
                    || discussion.getProblem() == null
                    || !discussion.getProblem().getId().equals(problem.getId())) {
                throw new RuntimeException("Invalid action. Could not find discussion inside the climbing problem.");
            }
        } else {
            List<DiscussionRoot> discussionRoots = discussionRootManager.findDiscussionRootByUserAndProblem(
                    requestUser, problem);
            discussion = discussionRoots.stream()
                    .filter(d -> d.getDiscussionId().equals(requestDiscussionId))
                    .findAny()
                    .orElse(null);
            if (discussion == null) {
                throw new RuntimeException("Invalid action. Could not find discussion inside the climbing problem.");
            }
        }

        boolean isExist;
        if (discussionContent.commentContent() != null){
            isExist = discussionRootManager.validateDiscussionCommentContent(discussion,
                    discussionContent.commentContent());
        } else {
            isExist = discussionRootManager.validateDiscussionBetaContent(discussion,
                    discussionContent.betaUrl());
        }
        if (!isExist){
            throw new RuntimeException("Could not find discussion with the given information.");
        }
    }
}
