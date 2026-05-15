package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.GradeDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.domain.model.ClimbingGrade;

import java.util.Optional;

@Service
@Transactional
public class ClimbingGradeManager {
    private final ClimbingGradeRepository climbingGradeRepository;

    public ClimbingGradeManager(ClimbingGradeRepository climbingGradeRepository) {
        this.climbingGradeRepository = climbingGradeRepository;
    }

    public ClimbingGrade getClimbingGrade(GradeDefinition grade){
        Optional<ClimbingGrade> climbingGrade = climbingGradeRepository.findByGradeDefinition(grade);
        return climbingGrade.orElse(null);
    }
}
