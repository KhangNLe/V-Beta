package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.UserComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link UserComment} entities.
 */
public interface UserCommentRepository extends JpaRepository<UserComment, Long> {
    /**
     * Returns user comment rows associated with a climbing problem.
     *
     * @param problem climbing problem
     * @return user comments for the problem
     */
    List<UserComment> findByClimbingProblem(ClimbingProblem problem);

    /**
     * Returns user-comment anchors authored by a specific account.
     *
     * @param userAccount author account
     * @return user comments for the account
     */
    List<UserComment> findByUserAccount(UserAccount userAccount);

    /**
     * Returns user-comment anchors for a user within a specific climbing problem.
     *
     * @param userAccount author account
     * @param problem climbing problem
     * @return matching user comments
     */
    List<UserComment> findByUserAccountAndClimbingProblem(UserAccount userAccount, ClimbingProblem problem);
}
