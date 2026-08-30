package app.VBeta.repository;

import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link DiscussionRoot} entities.
 */
public interface DiscussionRootRepository extends JpaRepository<DiscussionRoot, Long> {
    /**
     * Returns child discussion rows for a parent discussion id.
     *
     * @param parentDiscussionId parent discussion id
     * @return child discussion rows
     */
    List<DiscussionRoot> findByParent_DiscussionId(Long parentDiscussionId);

    /**
     * Returns child discussion rows for a parent discussion id, newest first.
     * <p>
     * Uses {@code discussionId} as a deterministic tie-breaker for equal timestamps.
     *
     * @param parentDiscussionId parent discussion id
     * @return child discussion rows ordered by create date descending then discussion id descending
     */
    List<DiscussionRoot> findByParent_DiscussionIdOrderByCreatedAtDescDiscussionIdDesc(Long parentDiscussionId);

    /**
     * Returns all discussion rows for a climbing problem in timeline order.
     * <p>
     * Uses {@code discussionId} as a deterministic tie-breaker for equal timestamps.
     *
     * @param problemId climbing problem id
     * @return discussion rows ordered by create date ascending then discussion id ascending
     */
    List<DiscussionRoot> findByProblem_IdOrderByCreatedAtAscDiscussionIdAsc(Long problemId);

    /**
     * Returns discussion rows authored by a user for a specific discussion type.
     *
     * @param userAccount author account
     * @param discussionType discussion kind
     * @return matching discussion rows
     */
    List<DiscussionRoot> findByUserAccount_AndDiscussionType(UserAccount userAccount, DiscussionType discussionType);

    /**
     * Returns discussion rows for a problem filtered by discussion type.
     *
     * @param problem climbing problem
     * @param discussionType discussion kind
     * @return matching discussion rows
     */
    List<DiscussionRoot> findByProblem_AndDiscussionType(ClimbingProblem problem, DiscussionType discussionType);

    /**
     * Returns discussion rows authored by a user on a specific problem.
     *
     * @param userAccount author account
     * @param problem climbing problem
     * @return matching discussion rows
     */
    List<DiscussionRoot> findByUserAccount_AndProblem(UserAccount userAccount, ClimbingProblem problem);

    /**
     * Returns top-level discussion rows for a problem, newest first.
     * <p>
     * Uses {@code discussionId} as a deterministic tie-breaker for equal timestamps.
     *
     * @param problemId climbing problem id
     * @return top-level discussion rows ordered by create date descending then discussion id descending
     */
    List<DiscussionRoot> findByProblem_IdAndParentIsNullOrderByCreatedAtDescDiscussionIdDesc(Long problemId);

    /**
     * Returns non-deleted child discussion rows for a parent, oldest first.
     * <p>
     * Uses {@code discussionId} as a deterministic tie-breaker for equal timestamps.
     *
     * @param parentDiscussionId parent discussion id
     * @return child rows with null deleted-at ordered by create date ascending then discussion id ascending
     */
    List<DiscussionRoot> findByParent_DiscussionIdAndDeletedAtIsNullOrderByCreatedAtAscDiscussionIdAsc(Long parentDiscussionId);

    /**
     * Returns non-deleted top-level discussion rows for a problem, newest first.
     * <p>
     * Uses {@code discussionId} as a deterministic tie-breaker for equal timestamps.
     *
     * @param problemId climbing problem id
     * @return top-level rows with null deleted-at ordered by create date descending then discussion id descending
     */
    List<DiscussionRoot> findByProblem_IdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDescDiscussionIdDesc(Long problemId);

    /**
     * Finds a non-deleted discussion that is not authored by the given user.
     *
     * @param discussionId discussion identifier
     * @param user reporter account to exclude as author
     * @return matching discussion when present
     */
    @Query("SELECT di FROM DiscussionRoot di WHERE di.discussionId = :discussionId AND di.deletedBy IS NULL "
            + "AND di.userAccount <> :user"
    )
    Optional<DiscussionRoot> findByDiscussionIdAndDeletedByIsNullAndNotFromUser(
            @Param("discussionId") Long discussionId,
            @Param("user") UserAccount user);
}
