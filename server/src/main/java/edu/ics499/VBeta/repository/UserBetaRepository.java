package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.UserBeta;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link UserBeta} entities.
 */
public interface UserBetaRepository extends JpaRepository<UserBeta, Long>{
    /**
     * Returns beta associations created by a user.
     *
     * @param userAccount user account
     * @return list of user-beta rows
     */
    List<UserBeta> findByUser(UserAccount userAccount);

    /**
     * Returns beta associations for a climbing problem.
     *
     * @param problem climbing problem
     * @return list of user-beta rows
     */
    List<UserBeta> findByProblem(ClimbingProblem problem);

    /**
     * Returns beta associations for a specific user/problem pair.
     *
     * @param userAccount user account
     * @param problem climbing problem
     * @return matching user-beta rows
     */
    List<UserBeta> findByUserAndProblem(UserAccount userAccount, ClimbingProblem problem);
}
