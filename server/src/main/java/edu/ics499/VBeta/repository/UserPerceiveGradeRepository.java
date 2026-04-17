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

public interface UserPerceiveGradeRepository extends JpaRepository<UserPerceiveGrade, UserPerceiveGradeId> {

    Optional<UserPerceiveGrade> findByUserAccountAndClimbingProblem(UserAccount userAccount, ClimbingProblem climbingProblem);

    @Query("SELECT DISTINCT up FROM UserPerceiveGrade up "
            + "JOIN FETCH up.climbingGrade JOIN FETCH up.climbingProblem "
            + "WHERE up.climbingProblem = :problem")
    List<UserPerceiveGrade> findByClimbingProblem(@Param("problem") ClimbingProblem problem);
}
