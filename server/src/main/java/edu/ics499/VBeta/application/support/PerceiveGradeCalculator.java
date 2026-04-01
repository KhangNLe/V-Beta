package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.ClimbingGrade;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.UserPerceiveGrade;
import edu.ics499.VBeta.repository.UserPerceiveGradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.*;

@Service
public class PerceiveGradeCalculator {
    private final UserPerceiveGradeRepository userPerceiveGradeRepository;

    public PerceiveGradeCalculator(UserPerceiveGradeRepository userPerceiveGradeRepository) {
        this.userPerceiveGradeRepository = userPerceiveGradeRepository;
    }

    /*
    This function will need to be redo later for a better algorithm of finding the mean grade value
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
        return (consensusGrade != null)? consensusGrade.getGrade() : " ";
    }
}
