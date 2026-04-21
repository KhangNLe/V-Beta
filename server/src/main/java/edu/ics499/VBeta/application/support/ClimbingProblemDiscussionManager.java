package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * {@code ClimbingProblemDiscussionManager} composes a unified discussion timeline for a climbing problem.
 * It merges text comments and beta video submissions into a shared
 * {@link UserCommentData} stream ordered by creation time.
 * <p>
 * Data is sourced from {@link DiscussionCommentManager} and {@link SolutionBetaManager}.
 */
@Service
public class ClimbingProblemDiscussionManager {
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;


    /**
     * Constructs a new {@code ClimbingProblemDiscussionManager} with discussion and beta collaborators.
     *
     * @param discussionCommentManager manager for text comment retrieval and persistence
     * @param solutionBetaManager manager for beta lookup and persistence
     */
    public ClimbingProblemDiscussionManager(
            DiscussionCommentManager discussionCommentManager,
            SolutionBetaManager solutionBetaManager){
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
    }

    /**
     * Returns merged comment and beta entries for a climbing problem in chronological order.
     *
     * @param problem climbing problem context
     * @return sorted discussion timeline entries
     */
    public List<UserCommentData> getCommentsForProblem(ClimbingProblem problem){
        List<UserCommentData> comments = new ArrayList<>();
        List<UserComment> commentsSrc = discussionCommentManager.getUserCommentFromClimbingProblem(problem);
        List<UserBeta> userBetas = solutionBetaManager.getUserBetasForClimbingProblem(problem);

        if (commentsSrc.isEmpty() && userBetas.isEmpty()){
            return comments;
        }

        getDiscussionComment(comments, commentsSrc);
        getSolutionBeta(comments, userBetas);

        return comments.stream().sorted(
                Comparator.comparing(UserCommentData::createdDate)
        ).toList();
    }

    private void getDiscussionComment(List<UserCommentData> comments, List<UserComment> commentsSrc){
        commentsSrc.forEach(src -> {
            DiscussionComment comment = discussionCommentManager.getDiscussionCommentByUserComment(src);
            if (comment != null){
                comments.add(new UserCommentData(
                        src.getUserAccount().getId(),
                        src.getUserAccount().getUsername(),
                        comment.getCommentInfo(),
                        null,
                        comment.getCreateDate()
                ));
            }
        });
    }

    private void getSolutionBeta(List<UserCommentData> comments, List<UserBeta> userBetas){
        userBetas.forEach(src -> {
            SolutionBeta beta = solutionBetaManager.getSolutionBetaFromUserBeta(src);
            if (beta != null){
                comments.add(new UserCommentData(
                        src.getUser().getId(),
                        src.getUser().getUsername(),
                        null,
                        beta.getVideoURL(),
                        beta.getCreateDate()
                ));
            }
        });
    }

    /**
     * Stores a user-authored discussion comment for a climbing problem.
     *
     * @param user author account
     * @param problem target climbing problem
     * @param commentInfo comment text content
     */
    public void storeDiscussionComment(UserAccount user, ClimbingProblem problem, String commentInfo){
        discussionCommentManager.storeDiscussionComment(user, problem,commentInfo);
    }

    public void removeUserComment(UserAccount userAccount, ClimbingProblem problem, String commentContent){
        discussionCommentManager.removeUserComment(userAccount, problem, commentContent);
    }
}
