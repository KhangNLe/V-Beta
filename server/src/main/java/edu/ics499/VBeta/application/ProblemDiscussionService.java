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
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        climbingProblemDiscussionManager.storeDiscussionComment(account, problem, request.commentInfo());
    }

    public CloudFileStorageResponse getSignedUrl(CloudFileStorageRequest request){
        return solutionBetaManager.createSignedUrl(request);
    }

    public void removeUserComment(String firebaseUid, CommentDeletionRequest request){
        UserAccount requestUser = userAccountManager.findUserAccount(firebaseUid);
        UserAccount commentAuthor = userAccountManager.findUserAccountById(request.authorId());
        ClimbingProblem problem = getActiveClimbingProblem(request.problemId());
        validateDeletionOwnerObject(requestUser, request.authorId());

        climbingProblemDiscussionManager.removeUserComment(commentAuthor, problem, request.commentContent());
    }

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
