package app.VBeta.application.support.grade;

import app.VBeta.domain.model.ClimbingGrade;
import app.VBeta.domain.model.GradeDefinition;
import app.VBeta.repository.ClimbingGradeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class ClimbingGradeManager {
    private final  ClimbingGradeRepository climbingGradeRepository;

    public ClimbingGradeManager(ClimbingGradeRepository climbingGradeRepository) {
        this.climbingGradeRepository = climbingGradeRepository;
    }

    public ClimbingGrade getClimbingGradeByDefinition(GradeDefinition gradeDefinition) {
        Optional<ClimbingGrade> grade = climbingGradeRepository.findByGradeDefinition(gradeDefinition);
        return grade.orElse(null);
    }
}
