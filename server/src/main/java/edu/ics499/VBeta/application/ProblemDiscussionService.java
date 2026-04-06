package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
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

    public ProblemDiscussionService(UserAccountManager userAccountManager,
                                    ClimbingProblemManager climbingProblemManager,
                                    ClimbingProblemDiscussionManager climbingProblemDiscussionManager){
        this.userAccountManager = userAccountManager;
        this.climbingProblemManager = climbingProblemManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
    }

    public void addComment(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(request.problemId());
        climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
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
