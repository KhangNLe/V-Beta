package app.VBeta.application.support.grade;

import app.VBeta.domain.model.ClimbingGrade;
import app.VBeta.domain.model.GradeDefinition;
import app.VBeta.repository.ClimbingGradeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * {@code ClimbingGradeManager} resolves {@link GradeDefinition} values to persisted
 * {@link ClimbingGrade} reference rows.
 * <p>
 * Used by discovery and problem workflows that need grade entities for range filters
 * and foreign-key associations.
 */
@Service
@Transactional
public class ClimbingGradeManager {
    private final  ClimbingGradeRepository climbingGradeRepository;

    /**
     * Constructs a new {@code ClimbingGradeManager} with a grade repository.
     *
     * @param climbingGradeRepository repository for climbing grade lookups
     */
    public ClimbingGradeManager(ClimbingGradeRepository climbingGradeRepository) {
        this.climbingGradeRepository = climbingGradeRepository;
    }

    /**
     * Returns the persisted climbing grade for a grade definition.
     *
     * @param gradeDefinition grade definition enum value
     * @return matching {@link ClimbingGrade}, or {@code null} when not found
     */
    public ClimbingGrade getClimbingGradeByDefinition(GradeDefinition gradeDefinition) {
        Optional<ClimbingGrade> grade = climbingGradeRepository.findByGradeDefinition(gradeDefinition);
        return grade.orElse(null);
    }
}
