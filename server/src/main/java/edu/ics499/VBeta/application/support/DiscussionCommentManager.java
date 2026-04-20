package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.UserBetaRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * {@code DiscussionCommentManager} manages write/read operations for discussion comments.
 * It persists a {@link UserComment} anchor and the associated {@link DiscussionComment} body
 * to model threaded discussion content per climbing problem.
 */
@Service
public class DiscussionCommentManager {
    private final UserCommentRepository userCommentRepository;
    private final DiscussionCommentRepository discussionCommentRepository;

    /**
     * Constructs a new {@code DiscussionCommentManager} with comment repositories.
     *
     * @param userCommentRepository repository for user comment anchors
     * @param discussionCommentRepository repository for discussion comment content
     */
    public DiscussionCommentManager(UserCommentRepository userCommentRepository,
                                    DiscussionCommentRepository discussionCommentRepository){
        this.userCommentRepository = userCommentRepository;
        this.discussionCommentRepository = discussionCommentRepository;
    }

    /**
     * Returns user comments linked to a climbing problem.
     *
     * @param problem climbing problem identifier context
     * @return list of user comments
     */
    public List<UserComment> getUserCommentFromClimbingProblem(ClimbingProblem problem){
        return userCommentRepository.findByClimbingProblem(problem);
    }

    /**
     * Returns the discussion comment associated with a user comment.
     *
     * @param userComment user comment parent record
     * @return discussion comment or {@code null} when not found
     */
    public DiscussionComment getDiscussionCommentByUserComment(UserComment userComment){
        Optional<DiscussionComment> comment = discussionCommentRepository.findByUserComment(userComment);
        return comment.orElse(null);
    }

    /**
     * Creates and stores a discussion comment authored by a user for a problem.
     *
     * @param user author account
     * @param problem target climbing problem
     * @param commentInfo discussion text content
     */
    public void storeDiscussionComment(UserAccount user, ClimbingProblem problem, String commentInfo){
        UserComment userComment = createNewUserComment(user, problem);
        createNewDiscussionComment(userComment, commentInfo);
    }

    private UserComment createNewUserComment(UserAccount user, ClimbingProblem problem){
        UserComment userComment = new UserComment();
        userComment.setUserAccount(user);
        userComment.setClimbingProblem(problem);
        return userCommentRepository.save(userComment);
    }

    private void createNewDiscussionComment(UserComment userComment, String commentInfo){
        DiscussionComment comment = new DiscussionComment();
        comment.setUserComment(userComment);
        comment.setCommentInfo(commentInfo);
        comment.setCreateDate(LocalDateTime.now());
        discussionCommentRepository.save(comment);
    }
}
