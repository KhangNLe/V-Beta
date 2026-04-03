package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.WallSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClimbingProblemRepository extends JpaRepository<ClimbingProblem, Long> {
    List<ClimbingProblem> findByWallSection(WallSection wallSection);
}
