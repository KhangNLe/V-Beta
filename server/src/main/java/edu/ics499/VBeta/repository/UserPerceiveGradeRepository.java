package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserPerceiveGrade;
import edu.ics499.VBeta.domain.model.UserPerceiveGradeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface UserPerceiveGradeRepository extends JpaRepository<UserPerceiveGrade, UserPerceiveGradeId> {
    List<UserPerceiveGrade> findByClimbingProblem(ClimbingProblem problem);
}
