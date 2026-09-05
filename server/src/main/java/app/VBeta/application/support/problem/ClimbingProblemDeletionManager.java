package app.VBeta.application.support.problem;

import app.VBeta.application.support.cloud.CloudStorageManager;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.grade.UserPerceiveGradeManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.repository.ClimbingProblemRepository;
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
    private final CloudStorageManager cloudStorageManager;


    /**
     * Constructs a new {@code ClimbingProblemDeletionManager} with repositories for all dependent entities.
     *
     * @param userPerceiveGradeManager manager for perceived-grade cleanup
     * @param solutionBetaManager manager for solution-beta cleanup
     * @param discussionCommentManager manager for discussion-comment cleanup
     * @param climbingProblemRepository repository for climbing problem entities
     * @param discussionRootManager manager for discussion-root cleanup
     */
    public ClimbingProblemDeletionManager(UserPerceiveGradeManager userPerceiveGradeManager,
                                          SolutionBetaManager solutionBetaManager,
                                          DiscussionCommentManager discussionCommentManager,
                                          ClimbingProblemRepository climbingProblemRepository,
                                          DiscussionRootManager discussionRootManager,
                                          CloudStorageManager cloudStorageManager){
        this.userPerceiveGradeManager = userPerceiveGradeManager;
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
        this.climbingProblemRepository = climbingProblemRepository;
        this.discussionRootManager = discussionRootManager;
        this.cloudStorageManager = cloudStorageManager;
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
        cloudStorageManager.deleteImageObject(problem.getObjectImageName());
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
