package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.api.dto.PerceiveGradeRequest;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.UserPerceiveGrade;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
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
    private final UserPerceiveGradeManager userPerceiveGradeManager;

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

    public void addComment(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(request.problemId());
        climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
    }

    public CloudFileStorageResponse getSignedUrl(CloudFileStorageRequest request){
        return solutionBetaManager.createSignedUrl(request);
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount account =  userAccountManager.findUserAccount(firebaseUid);
        if (account == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User Account with the unique firebase ID does not exist. Please log in and try again.");
        }
        return account;
    }

    public void addClimbingProblemPerceiveGrade(String firebaseUid, Long problemId, PerceiveGradeRequest request){
        ClimbingProblem problem = getClimbingProblem(problemId);
        userPerceiveGradeManager.addPerceiveGrade(problem, firebaseUid, request.perceiveGrade());
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
