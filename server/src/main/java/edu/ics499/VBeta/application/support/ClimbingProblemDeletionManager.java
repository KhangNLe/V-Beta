package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@code ClimbingProblemDeletionManager} performs cascading deletion for a
 * {@link ClimbingProblem} and all dependent records.
 * <p>
 * The manager explicitly removes related betas, comments, and perceived grades in a controlled
 * sequence before deleting the problem itself, ensuring referential integrity at the application level.
 */
@Service
@Transactional
public class ClimbingProblemDeletionManager {
    private final UserPerceiveGradeManager userPerceiveGradeManager;
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;
    private final ClimbingProblemRepository climbingProblemRepository;
    private final DiscussionRootManager discussionRootManager;


    /**
     * Constructs a new {@code ClimbingProblemDeletionManager} with repositories for all dependent entities.
     *
     * @param climbingProblemRepository repository for climbing problem entities
     */
    public ClimbingProblemDeletionManager(UserPerceiveGradeManager userPerceiveGradeManager,
                                          SolutionBetaManager solutionBetaManager,
                                          DiscussionCommentManager discussionCommentManager,
                                          ClimbingProblemRepository climbingProblemRepository,
                                          DiscussionRootManager discussionRootManager){
        this.userPerceiveGradeManager = userPerceiveGradeManager;
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
        this.climbingProblemRepository = climbingProblemRepository;
        this.discussionRootManager = discussionRootManager;
    }

    /**
     * Deletes a climbing problem and associated comments, beta records, and perceived grades.
     *
     * @param problem climbing problem to delete
     */
    public void deleteClimbingProblem(ClimbingProblem problem){
        if(validateClimbingProblem(problem)){
            return ;
        }

        deleteProblemDiscussions(problem);
        deleteProblemPerceiveGrades(problem);
        climbingProblemRepository.delete(problem);
    }

    private boolean validateClimbingProblem(ClimbingProblem problem){
        return problem == null;
    }

    private void deleteProblemDiscussions(ClimbingProblem problem){
        List<DiscussionRoot> discussionComments = discussionRootManager.getDiscussionsByProblemAndType(problem,
                DiscussionType.COMMENT);
        List<DiscussionRoot> discussionBetas = discussionRootManager.getDiscussionsByProblemAndType(problem,
                DiscussionType.BETA);
        discussionCommentManager.removeAllDiscussionRelatedComments(discussionComments);
        solutionBetaManager.removeAllDiscussionRelatedSolutionBeta(discussionBetas);

        //join the two list for group deletion
        discussionComments.addAll(discussionBetas);
        discussionRootManager.removeDiscussions(discussionComments);
    }

    private void deleteProblemPerceiveGrades(ClimbingProblem problem){
        userPerceiveGradeManager.removeProblemRelatedPerceiveGrade(problem);
    }
}
