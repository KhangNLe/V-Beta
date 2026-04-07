package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ClimbingProblemDiscussionManager {
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;

    public ClimbingProblemDiscussionManager(
            DiscussionCommentManager discussionCommentManager,
            SolutionBetaManager solutionBetaManager){
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
    }

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
}
