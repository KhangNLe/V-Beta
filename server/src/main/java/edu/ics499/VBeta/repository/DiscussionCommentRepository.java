package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.domain.model.UserComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscussionCommentRepository extends JpaRepository<DiscussionComment, Long> {
    Optional<DiscussionComment> findByUserComment (UserComment userComment);
    List<DiscussionComment> findByCommentInfoAndUserCommentInOrderByCreateDateDesc(
            String commentInfo, List<UserComment> comments
    );
}
