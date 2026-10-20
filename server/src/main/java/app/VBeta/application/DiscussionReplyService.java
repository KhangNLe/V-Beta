package app.VBeta.application;

import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import app.VBeta.api.dto.discussions.DiscussionContentReplyRequest;
import app.VBeta.application.NotificationService;

import java.util.List;

/**
 * {@code DiscussionReplyService} orchestrates creating replies under an existing discussion root.
 * <p>
 * A reply may be comment-only, beta-only, or mixed. The parent must be an active (non-deleted)
 * discussion on the same problem, and {@code replyToId} must match the parent's author.
 * After a successful create, a {@code DISCUSSION_REPLY} inbox notification is sent to the
 * parent author unless the replier is that author.
 */
@Service
public class DiscussionReplyService {
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final NotificationService notificationService;
    private final DiscussionRootManager discussionRootManager;

    /**
     * Constructs a new {@code DiscussionReplyService} with required collaborators.
     *
     * @param climbingProblemDiscussionManager manager that persists reply comment/beta/mixed rows
     * @param userAccountManager manager for author and reply-to account lookups
     * @param climbingProblemManager manager for active problem lookups
     * @param notificationService service that writes reply inbox notifications
     * @param discussionRootManager manager for parent and reply discussion roots
     */
    public DiscussionReplyService(ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
                                  UserAccountManager userAccountManager,
                                  ClimbingProblemManager climbingProblemManager,
                                  NotificationService notificationService,
                                  DiscussionRootManager discussionRootManager) {
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.notificationService = notificationService;
        this.discussionRootManager = discussionRootManager;
    }

    /**
     * Stores a reply for the authenticated user and optionally notifies the parent author.
     * <p>
     * Content kind is inferred from the request: both video fields set with comment → mixed;
     * comment only → comment; both video fields without comment → beta. Incomplete video
     * pairs, mismatched parent metadata, duplicates, missing/deleted parents, and unknown
     * users/problems are rejected.
     *
     * @param firebaseUid Firebase UID of the reply author
     * @param request reply payload including parent discussion, problem, and content fields
     * @return created timeline entry with {@code parentDiscussionId} set
     * @throws IllegalArgumentException when the payload is incomplete, mismatched, or duplicate
     * @throws RuntimeException when the user, problem, or parent discussion cannot be used
     */
    public UserDiscussionData storeUserReply(String firebaseUid, DiscussionContentReplyRequest request){
        UserAccount author = getUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveProblem(request.problemId());
        DiscussionRoot parentRoot = findParentDiscussionRoot(request.parentDiscussionId());
        verifyRequest(parentRoot, problem, request.replyToId());
        checkForDuplicateReply(author, problem, request);

        UserDiscussionData reply;
        if (request.commentInfo() == null){
            if (request.objectFileName() == null || request.videoURL() == null){
                throw new IllegalArgumentException("Missing require field for request");
            }
            reply = climbingProblemDiscussionManager.storeReplySolutionBeta(author, problem, request.objectFileName(),
                    request.videoURL(), parentRoot);
        } else if (request.objectFileName() != null && request.videoURL() != null){
            reply = climbingProblemDiscussionManager.storeReplyMixedDiscussion(author, problem, request, parentRoot);
        } else {
            if ((request.objectFileName() == null && request.videoURL() != null) ||
                    (request.objectFileName() != null && request.videoURL() == null)) {
                throw new IllegalArgumentException("Missing require field for request");
            }
            reply = climbingProblemDiscussionManager.storeReplyComment(author, problem, request.commentInfo(),
                    parentRoot);
        }

        DiscussionRoot root = discussionRootManager.findDiscussionRootById(reply.discussionId());
        if (!author.equals(parentRoot.getUserAccount())) {
            notificationService.saveNewDiscussionReplyNotification(author, parentRoot.getUserAccount(), root);
        }
        return reply;
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount user =  userAccountManager.findUserAccount(firebaseUid);
        if (user == null){
            throw new RuntimeException("Unknown firebaseUid");
        }
        return user;
    }

    private ClimbingProblem getActiveProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new RuntimeException("Problem not found or may have been archive.");
        }
        return problem;
    }

    private DiscussionRoot findParentDiscussionRoot(Long discussionId){
        DiscussionRoot root = discussionRootManager.findDiscussionRootById(discussionId);
        if (root == null || root.getDeletedAt() != null){
            throw new RuntimeException("Discussion not found from provided ID.");
        }
        return root;
    }

    private void verifyRequest(DiscussionRoot parentRoot, ClimbingProblem problem, Long replyToId){
        UserAccount user = userAccountManager.findUserAccountById(replyToId);
        if (!parentRoot.getUserAccount().equals(user) || !parentRoot.getProblem().equals(problem)){
            throw new IllegalArgumentException("Mismatching information.");
        }
    }

    private void checkForDuplicateReply(UserAccount user, ClimbingProblem problem,
                                        DiscussionContentReplyRequest request){
        List<DiscussionRoot> discussions = discussionRootManager.findDiscussionRootByUserAndProblem(user, problem);
        UserDiscussionData data;
        for (DiscussionRoot discussion : discussions){
            data = discussionRootManager.getUserDiscussionData(discussion);
            if (data.parentDiscussionId() != null && data.parentDiscussionId().equals(request.parentDiscussionId())){
                boolean isCommentDuplicate = data.comment() != null && data.comment().equals(request.commentInfo());
                boolean isBetaDuplicate = data.videoURL() != null && data.videoURL().equals(request.videoURL());

                if ((isCommentDuplicate && isBetaDuplicate) || (request.commentInfo() == null && isBetaDuplicate) ||
                        (request.videoURL() == null && isCommentDuplicate)){
                    throw new IllegalArgumentException("Duplicate discussion found.");
                }
            }
        }
    }
}
