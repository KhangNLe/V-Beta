package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.domain.model.UserComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link DiscussionComment} entities.
 */
public interface DiscussionCommentRepository extends JpaRepository<DiscussionComment, Long> {
    /**
     * Finds a discussion comment by its user-comment anchor row.
     *
     * @param userComment user-comment anchor
     * @return discussion comment when present
     */
    Optional<DiscussionComment> findByUserComment (UserComment userComment);

    /**
     * Finds discussion comments for a batch of user-comment anchors.
     *
     * @param userComments user-comment anchors
     * @return matching discussion comments
     */
    List<DiscussionComment> findByUserCommentIn(List<UserComment> userComments);

    /**
     * Finds matching discussion comments by text and candidate user comments,
     * ordered newest first.
     *
     * @param commentInfo discussion comment text to match
     * @param comments candidate user-comment anchors
     * @return matching discussion comments ordered by create date descending
     */
    List<DiscussionComment> findByCommentInfoAndUserCommentInOrderByCreateDateDesc(
            String commentInfo, List<UserComment> comments
    );
}
