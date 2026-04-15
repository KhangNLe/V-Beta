package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.UserComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCommentRepository extends JpaRepository<UserComment, Long> {
    List<UserComment> findByClimbingProblem(ClimbingProblem problem);
    List<UserComment> findByUserAccountAndClimbingProblem(UserAccount userAccount, ClimbingProblem problem);
}
