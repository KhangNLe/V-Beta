package app.VBeta.application.support.grade;

import app.VBeta.domain.model.climb.ClimbingGrade;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.user.UserPerceiveGrade;
import app.VBeta.repository.UserPerceiveGradeRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * {@code PerceiveGradeCalculator} computes consensus perceived grade output for a
 * {@link ClimbingProblem}.
 * <p>
 * The current strategy picks the most frequently selected {@link ClimbingGrade} from
 * user submissions.
 */
@Service
public class PerceiveGradeCalculator {
    private final UserPerceiveGradeRepository userPerceiveGradeRepository;

    /**
     * Constructs a new {@code PerceiveGradeCalculator} with perceived grade repository access.
     *
     * @param userPerceiveGradeRepository repository for user-submitted perceived grades
     */
    public PerceiveGradeCalculator(UserPerceiveGradeRepository userPerceiveGradeRepository) {
        this.userPerceiveGradeRepository = userPerceiveGradeRepository;
    }

    /**
     * Returns the current consensus perceived grade for a climbing problem.
     *
     * <p>Consensus is determined by the most frequently selected grade.
     *
     * @param problem climbing problem to evaluate
     * @return grade name when present, otherwise a blank marker
     */
    public String findPerceiveGrade(ClimbingProblem problem){
        List<UserPerceiveGrade> perceiveGrades = userPerceiveGradeRepository.findByClimbingProblem(problem);
        if (perceiveGrades.isEmpty()){
            return " ";
        }
        HashMap<ClimbingGrade, Integer> consensus = new HashMap<>();
        perceiveGrades.forEach(e -> {
            consensus.put(
                    e.getClimbingGrade(),
                    consensus.getOrDefault(e.getClimbingGrade(), 0) + 1);
        });
        ClimbingGrade consensusGrade = consensus.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        return (consensusGrade != null)? consensusGrade.getGradeDefinition().name() : " ";
    }
}
