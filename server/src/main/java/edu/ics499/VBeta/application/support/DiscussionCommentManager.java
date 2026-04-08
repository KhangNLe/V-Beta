package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.domain.model.UserBeta;
import edu.ics499.VBeta.domain.model.UserComment;
import edu.ics499.VBeta.repository.UserBetaRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class DiscussionCommentManager {
    private final UserCommentRepository userCommentRepository;
    private final DiscussionCommentRepository discussionCommentRepository;

    public DiscussionCommentManager(UserCommentRepository userCommentRepository,
                                    DiscussionCommentRepository discussionCommentRepository){
        this.userCommentRepository = userCommentRepository;
        this.discussionCommentRepository = discussionCommentRepository;
    }

    public List<UserComment> getUserCommentFromClimbingProblem(ClimbingProblem problem){
        return userCommentRepository.findByClimbingProblem(problem);
    }

    public DiscussionComment getDiscussionCommentByUserComment(UserComment userComment){
        Optional<DiscussionComment> comment = discussionCommentRepository.findByUserComment(userComment);
        return comment.orElse(null);
    }
}
