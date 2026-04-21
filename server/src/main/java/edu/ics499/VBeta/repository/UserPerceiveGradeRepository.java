package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.UserPerceiveGrade;
import edu.ics499.VBeta.domain.model.UserPerceiveGradeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link UserPerceiveGrade} persistence and query operations.
 */
public interface UserPerceiveGradeRepository extends JpaRepository<UserPerceiveGrade, UserPerceiveGradeId> {

    /**
     * Finds a user's perceived grade entry for a specific climbing problem.
     *
     * @param userAccount user account
     * @param climbingProblem climbing problem
     * @return matching perceived-grade row when present
     */
    Optional<UserPerceiveGrade> findByUserAccountAndClimbingProblem(UserAccount userAccount, ClimbingProblem climbingProblem);

    /**
     * Returns perceived grade entries for a climbing problem with related grade/problem eagerly fetched.
     *
     * @param problem climbing problem to query
     * @return perceived grade rows for the problem
     */
    @Query("SELECT DISTINCT up FROM UserPerceiveGrade up "
            + "JOIN FETCH up.climbingGrade JOIN FETCH up.climbingProblem "
            + "WHERE up.climbingProblem = :problem")
    List<UserPerceiveGrade> findByClimbingProblem(@Param("problem") ClimbingProblem problem);
}
