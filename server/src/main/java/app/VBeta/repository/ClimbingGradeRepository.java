package app.VBeta.repository;

import app.VBeta.domain.model.ClimbingGrade;
import app.VBeta.domain.model.GradeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link ClimbingGrade} reference data.
 */
public interface ClimbingGradeRepository extends JpaRepository<ClimbingGrade, Long> {
    /**
     * Finds a climbing grade by enumerated grade definition.
     *
     * @param grade grade definition enum
     * @return matching climbing grade when present
     */
    Optional<ClimbingGrade> findByGradeDefinition(GradeDefinition grade);
}
