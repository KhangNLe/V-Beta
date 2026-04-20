package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.domain.model.UserComment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
