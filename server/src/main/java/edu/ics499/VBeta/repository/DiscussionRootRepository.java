package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.DiscussionRoot;
import edu.ics499.VBeta.domain.model.DiscussionType;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionRootRepository extends JpaRepository<DiscussionRoot, Long> {
    List<DiscussionRoot> findByParent_DiscussionId(Long parentDiscussionId);
    List<DiscussionRoot> findByParent_DiscussionIdOrderByCreatedAtDesc(Long parentDiscussionId);
    List<DiscussionRoot> findByProblem(ClimbingProblem problem);
    List<DiscussionRoot> findByUserAccount_AndDiscussionType(UserAccount userAccount, DiscussionType discussionType);
    List<DiscussionRoot> findByProblem_AndDiscussionType(ClimbingProblem problem, DiscussionType discussionType);

    List<DiscussionRoot> findByProblem_IdAndParentIsNullOrderByCreatedAtDesc(Long problemId);
    List<DiscussionRoot> findByParent_DiscussionIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentDiscussionId);

    List<DiscussionRoot> findByProblem_IdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long problemId);
}
