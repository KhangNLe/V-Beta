package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * {@code DiscussionCommentManager} manages write/read operations for discussion comments.
 * It persists and resolves {@link DiscussionComment} records keyed by {@link DiscussionRoot}
 * to model threaded discussion content per discussion item.
 * <p>
 * It also supports targeted and bulk deletion for discussion roots.
 */
@Service
public class DiscussionCommentManager {
    private final DiscussionCommentRepository discussionCommentRepository;

    /**
     * Constructs a new {@code DiscussionCommentManager} with comment repository.
     *
     * @param discussionCommentRepository repository for discussion comment content
     */
    public DiscussionCommentManager(DiscussionCommentRepository discussionCommentRepository){
        this.discussionCommentRepository = discussionCommentRepository;
    }

    /**
     * Returns the discussion comment associated with a discussion root.
     *
     * @param discussionRoot discussion root parent record
     * @return discussion comment or {@code null} when not found
     */
    public DiscussionComment getDiscussionComment(DiscussionRoot discussionRoot){
        Optional<DiscussionComment> comment = discussionCommentRepository.findByDiscussionRoot(discussionRoot);
        return comment.orElse(null);
    }

    /**
     * Creates and stores a discussion comment for a discussion root.
     *
     * @param discussionRoot discussion root parent record
     * @param commentInfo discussion text content
     */
    public void storeDiscussionComment(DiscussionRoot discussionRoot, String commentInfo){
        createNewDiscussionComment(discussionRoot, commentInfo);
    }

    private void createNewDiscussionComment(DiscussionRoot discussionRoot, String commentInfo){
        DiscussionComment comment = new DiscussionComment();
        comment.setDiscussionRoot(discussionRoot);
        comment.setCommentInfo(commentInfo);
        comment.setCreateDate(LocalDateTime.now());
        discussionCommentRepository.save(comment);
    }

    /**
     * Removes the discussion comment associated with a discussion root.
     *
     * @param discussionRoot discussion root parent record
     */
    public void removeUserComment(DiscussionRoot discussionRoot) {
        DiscussionComment discussionComment = findDiscussionComment(discussionRoot);
        discussionCommentRepository.delete(discussionComment);
    }

    /**
     * Removes all discussion-comment rows for the provided discussion roots.
     *
     * @param discussionRoots discussion roots whose comment rows should be removed
     */
    public void removeAllDiscussionRelatedComments(List<DiscussionRoot> discussionRoots){
        List<DiscussionComment> discussionComments = getDiscussionComments(discussionRoots);
        discussionCommentRepository.deleteAll(discussionComments);
    }


    /**
     * Resolves discussion comments for a set of discussion roots and enforces
     * one-to-one mapping cardinality.
     *
     * @param discussionRoots discussion roots expected to have matching discussion rows
     * @return discussion-comment rows matching each anchor
     * @throws ResponseStatusException with {@link HttpStatus#INTERNAL_SERVER_ERROR}
     * when one or more discussion rows are missing
     */
    private List<DiscussionComment> getDiscussionComments(List<DiscussionRoot> discussionRoots){
        List<DiscussionComment> discussionComments = discussionCommentRepository.findByDiscussionRootIn(discussionRoots);
        if (discussionComments.size() != discussionRoots.size()){
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format(
                            "Mismatching size between user comments %d and discussion comment %d for user comment id %s."
                            + " Please contact the developer for this issue",
                            discussionRoots.size(), discussionComments.size(),
                            discussionRoots.get(0).getUserAccount().getId()
                    )
            );
        }
        return discussionComments;
    }

    /**
     * Finds the discussion comment associated with the given discussion root.
     *
     * @param discussionRoot discussion root parent record
     * @return matching discussion comment
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no matching comment is found
     */
    private DiscussionComment findDiscussionComment(DiscussionRoot discussionRoot){
        Optional<DiscussionComment> discussionComment = discussionCommentRepository.findByDiscussionRoot(discussionRoot);
        return discussionComment.orElseThrow(()->
             new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                String.format("Could not find comment with the discussion id %d", discussionRoot.getDiscussionId())
            )
        );
    }
}
