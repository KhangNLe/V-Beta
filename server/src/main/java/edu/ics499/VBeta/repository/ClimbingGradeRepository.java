package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.ClimbingGrade;
import edu.ics499.VBeta.domain.model.GradeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClimbingGradeRepository extends JpaRepository<ClimbingGrade, Long> {
    Optional<ClimbingGrade> findByGradeDefinition(GradeDefinition grade);
}
