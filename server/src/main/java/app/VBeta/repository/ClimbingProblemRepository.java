package app.VBeta.repository;

import app.VBeta.domain.model.ClimbingProblem;
import app.VBeta.domain.model.WallSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link ClimbingProblem} entities.
 */
public interface ClimbingProblemRepository extends JpaRepository<ClimbingProblem, Long> {
    /**
     * Returns climbing problems associated with a wall section.
     *
     * @param wallSection wall section
     * @return climbing problems for the wall section
     */
    List<ClimbingProblem> findByWallSection(WallSection wallSection);
}
