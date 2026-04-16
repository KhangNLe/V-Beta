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

    public void removeUserComment(UserAccount user, ClimbingProblem problem, String commentContent) {
        List<UserComment> comments = getUserComment(user, problem);
        DiscussionComment discussionComment = findDiscussionComment(comments, commentContent);
        UserComment deletingUserComment = discussionComment.getUserComment();
        discussionCommentRepository.delete(discussionComment);
        userCommentRepository.delete(deletingUserComment);
    }

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
