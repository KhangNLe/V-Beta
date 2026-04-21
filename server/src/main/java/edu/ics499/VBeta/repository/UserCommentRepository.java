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
    List<UserComment> findByUserAccountAndClimbingProblem(UserAccount userAccount, ClimbingProblem problem);
}
