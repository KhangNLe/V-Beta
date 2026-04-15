package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.api.dto.CommentDeletionRequest;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.SolutionBetaManager;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import edu.ics499.VBeta.application.support.UserAccountManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProblemDiscussionService {
    private final UserAccountManager userAccountManager;
    private final ClimbingProblemManager climbingProblemManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final SolutionBetaManager solutionBetaManager;

    public ProblemDiscussionService(UserAccountManager userAccountManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
                                    SolutionBetaManager solutionBetaManager){
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.solutionBetaManager = solutionBetaManager;
    }

    public void addComment(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(request.problemId());
        climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
    }

    public CloudFileStorageResponse getSignedUrl(CloudFileStorageRequest request){
        return solutionBetaManager.createSignedUrl(request);
    }

    public void removeUserComment(String firebaseUid, CommentDeletionRequest request){
        UserAccount requestUser = userAccountManager.findUserAccount(firebaseUid);
        UserAccount commentAuthor = userAccountManager.findUserAccountById(request.authorId());
       ClimbingProblem problem = getClimbingProblem(request.problemId());

        //There will be a permission check on here that is current on another PR
        climbingProblemDiscussionManager.removeUserComment(commentAuthor, problem, request.commentContent());
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount account =  userAccountManager.findUserAccount(firebaseUid);
        if (account == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User Account with the unique firebase ID does not exist. Please log in and try again.");
        }
        return account;
    }

    private ClimbingProblem getClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("The problem with ID %d does not exist or no longer active.", problemId));
        }
        return problem;
    }
}
