package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.DiscussionRoot;
import edu.ics499.VBeta.domain.model.DiscussionType;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
     *
     * @param parentDiscussionId parent discussion id
     * @return child discussion rows ordered by create date descending
     */
    List<DiscussionRoot> findByParent_DiscussionIdOrderByCreatedAtDesc(Long parentDiscussionId);

    /**
     * Returns all discussion rows for a climbing problem.
     *
     * @param problem climbing problem
     * @return discussion rows for the problem
     */
    List<DiscussionRoot> findByProblem(ClimbingProblem problem);

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
     *
     * @param problemId climbing problem id
     * @return top-level discussion rows ordered by create date descending
     */
    List<DiscussionRoot> findByProblem_IdAndParentIsNullOrderByCreatedAtDesc(Long problemId);

    /**
     * Returns non-deleted child discussion rows for a parent, oldest first.
     *
     * @param parentDiscussionId parent discussion id
     * @return child rows with null deleted-at ordered by create date ascending
     */
    List<DiscussionRoot> findByParent_DiscussionIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentDiscussionId);

    /**
     * Returns non-deleted top-level discussion rows for a problem, newest first.
     *
     * @param problemId climbing problem id
     * @return top-level rows with null deleted-at ordered by create date descending
     */
    List<DiscussionRoot> findByProblem_IdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long problemId);
}
