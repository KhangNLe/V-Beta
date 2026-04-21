package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.UserCommentRepository;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * {@code DiscussionCommentManager} manages write/read operations for discussion comments.
 * It persists a {@link UserComment} anchor and the associated {@link DiscussionComment} body
 * to model threaded discussion content per climbing problem.
 * <p>
 * It also supports targeted deletion by finding the latest matching comment text for a
 * user/problem context.
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

    /**
     * Removes a user's discussion comment for a problem by matching comment content.
     *
     * @param user author account
     * @param problem climbing problem context
     * @param commentContent comment text to match for deletion
     */
    public void removeUserComment(UserAccount user, ClimbingProblem problem, String commentContent) {
        List<UserComment> comments = getUserComment(user, problem);
        DiscussionComment discussionComment = findDiscussionComment(comments, commentContent);
        UserComment deletingUserComment = discussionComment.getUserComment();
        discussionCommentRepository.delete(discussionComment);
        userCommentRepository.delete(deletingUserComment);
    }

    /**
     * Returns user-comment anchors for a specific user/problem pair.
     *
     * @param userAccount author account
     * @param climbingProblem climbing problem context
     * @return list of matching user comments
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no comments exist
     */
    private List<UserComment> getUserComment(UserAccount userAccount, ClimbingProblem climbingProblem){
        List<UserComment> userComments = userCommentRepository.findByUserAccountAndClimbingProblem(userAccount, climbingProblem);
        if (userComments.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Could not find any comment for problem %d from user: %s.",
                            climbingProblem.getId(), userAccount.getUsername())
            );
        }
        return userComments;
    }

    /**
     * Finds the latest discussion comment matching the requested content.
     *
     * @param userComments candidate user-comment anchors
     * @param commentContent comment text to match
     * @return most recent matching discussion comment
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no matching comment is found
     */
    private DiscussionComment findDiscussionComment(List<UserComment> userComments, String commentContent){
        List<DiscussionComment> discussionComment = discussionCommentRepository.
                findByCommentInfoAndUserCommentInOrderByCreateDateDesc(
                    commentContent, userComments
        );

        if (discussionComment.isEmpty()){
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                String.format("Could not find comment from user that match %s.", commentContent)
            );
        }

        return discussionComment.get(0);
    }
}
