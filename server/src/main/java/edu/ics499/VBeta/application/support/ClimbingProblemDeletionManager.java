package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClimbingProblemDeletionManager {
    private final UserPerceiveGradeRepository userPerceiveGradeRepository;
    private final SolutionBetaRepository solutionBetaRepository;
    private final UserBetaRepository userBetaRepository;
    private final UserCommentRepository userCommentRepository;
    private final DiscussionCommentRepository discussionCommentRepository;
    private final ClimbingProblemRepository climbingProblemRepository;


    public ClimbingProblemDeletionManager(UserPerceiveGradeRepository userPerceiveGradeRepository,
                                          SolutionBetaRepository solutionBetaRepository,
                                          UserBetaRepository userBetaRepository,
                                          UserCommentRepository userCommentRepository,
                                          DiscussionCommentRepository discussionCommentRepository,
                                          ClimbingProblemRepository climbingProblemRepository){
        this.userPerceiveGradeRepository = userPerceiveGradeRepository;
        this.userBetaRepository = userBetaRepository;
        this.discussionCommentRepository = discussionCommentRepository;
        this.solutionBetaRepository = solutionBetaRepository;
        this.userCommentRepository = userCommentRepository;
        this.climbingProblemRepository = climbingProblemRepository;
    }

    public void deleteClimbingProblem(ClimbingProblem problem){
        if(validateClimbingProblem(problem)){
            return ;
        }

        deleteUserRelatedBeta(problem);
        deleteUserRelatedComment(problem);
        deleteProblemPerceiveGrades(problem);
        climbingProblemRepository.delete(problem);
    }

    private boolean validateClimbingProblem(ClimbingProblem problem){
        return problem == null;
    }

    private void deleteUserRelatedBeta(ClimbingProblem problem){
        List<UserBeta> userBetas = userBetaRepository.findByProblem(problem);
        if (userBetas.isEmpty()) return;
        userBetas.forEach(this::deleteSolutionBeta);
        userBetaRepository.deleteAll(userBetas);
    }

    private void deleteSolutionBeta(UserBeta userBeta){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByUserBeta(userBeta);
        if (solutionBeta.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Error while deleting solution beta for user beta %d, please contact the developers.",
                            userBeta.getId())
            );
        }

        solutionBetaRepository.delete(solutionBeta.get());
    }

    private void deleteUserRelatedComment(ClimbingProblem problem){
        List<UserComment> userComments = userCommentRepository.findByClimbingProblem(problem);
        if (userComments.isEmpty()) return;

        userComments.forEach(this::deleteDiscussionComment);
        userCommentRepository.deleteAll(userComments);
    }

    private void deleteDiscussionComment(UserComment userComment){
        Optional<DiscussionComment> comment = discussionCommentRepository.findByUserComment(userComment);
        if (comment.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Error while deleting discussion comment for user comment %d, please contact the developers.",
                            userComment.getUserCommentId())
            );
        }

        discussionCommentRepository.delete(comment.get());
    }

    private void deleteProblemPerceiveGrades(ClimbingProblem problem){
        List<UserPerceiveGrade> userPerceiveGrades = userPerceiveGradeRepository.findByClimbingProblem(problem);
        if (userPerceiveGrades.isEmpty()) return;

        userPerceiveGradeRepository.deleteAll(userPerceiveGrades);
    }
}
