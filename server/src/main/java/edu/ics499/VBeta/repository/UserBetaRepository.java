package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.UserBeta;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBetaRepository extends JpaRepository<UserBeta, Long>{
    List<UserBeta> findByUser(UserAccount userAccount);
    List<UserBeta> findByProblem(ClimbingProblem problem);
    List<UserBeta> findByUserAndProblem(UserAccount userAccount, ClimbingProblem problem);
}
