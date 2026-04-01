package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserComment;
import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClimbingProblemDiscussionManager {
    private final DiscussionCommentRepository discussionCommentRepository;
    private final UserCommentRepository userCommentRepository;

    public ClimbingProblemDiscussionManager(
            DiscussionCommentRepository discussionCommentRepository,
            UserCommentRepository userCommentRepository){
        this.discussionCommentRepository = discussionCommentRepository;
        this.userCommentRepository = userCommentRepository;
    }

    public List<UserCommentData> getCommentsForProblem(ClimbingProblem problem){
        List<UserCommentData> comments = new ArrayList<>();
        List<UserComment> commentsSrc = userCommentRepository.findByClimbingProblem(problem);

        if (commentsSrc.isEmpty()){
            return comments;
        }

        commentsSrc.forEach(src -> {
            Optional<DiscussionComment> commentInfo = discussionCommentRepository.findByUserComment(src);
            commentInfo.ifPresent(comment -> comments.add(new UserCommentData(
                    src.getUserAccount().getId(),
                    src.getUserAccount().getUsername(),
                    comment.getCommentInfo(),
                    null,
                    comment.getCreateDate()
            )));
        });

        return comments;
    }
}
