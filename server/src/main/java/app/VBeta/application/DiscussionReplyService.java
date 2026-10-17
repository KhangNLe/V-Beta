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

@Service
public class DiscussionReplyService {
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final NotificationService notificationService;
    private final DiscussionRootManager discussionRootManager;


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

    public UserDiscussionData storeUserReply(String firebaseUid, DiscussionContentReplyRequest request){
        UserAccount author = getUserAccount(firebaseUid);
        ClimbingProblem problem = getActiveProblem(request.problemId());
        DiscussionRoot parentRoot = findParentDiscussionRoot(request.parentDiscussionId());
        verifyRequest(parentRoot, problem, request.replyToId());

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
            reply = climbingProblemDiscussionManager.storeReplyComment(author, problem, request.commentInfo(),
                    parentRoot);
        }

        DiscussionRoot root = discussionRootManager.findDiscussionRootById(reply.discussionId());
        notificationService.saveNewDiscussionReplyNotification(author, parentRoot.getUserAccount(), root);
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
        if (root == null){
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
}
