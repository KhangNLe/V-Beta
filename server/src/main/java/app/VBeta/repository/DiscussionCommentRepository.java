package app.VBeta.repository;

import app.VBeta.domain.model.discussions.DiscussionComment;
import app.VBeta.domain.model.discussions.DiscussionRoot;
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
     * @param discussionRoot candidate discussion root anchors
     * @return discussion comment when present
     */
    Optional<DiscussionComment> findByDiscussionRoot (DiscussionRoot discussionRoot);

    /**
     * Finds discussion comments for a batch of user-comment anchors.
     *
     * @param discussionRoots candidate discussion root anchors
     * @return matching discussion comments
     */
    List<DiscussionComment> findByDiscussionRootIn(List<DiscussionRoot> discussionRoots);

    /**
     * Finds matching discussion comments by text and candidate user comments,
     * ordered newest first.
     *
     * @param commentInfo discussion comment text to match
     * @param discussionRoots candidate discussion root anchors
     * @return matching discussion comments ordered by create date descending
     */
    List<DiscussionComment> findByCommentInfoAndDiscussionRootInOrderByCreateDateDesc(
            String commentInfo, List<DiscussionRoot> discussionRoots
    );
}
