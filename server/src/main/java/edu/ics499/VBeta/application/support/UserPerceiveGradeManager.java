package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.PerceiveGradeRequest;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.repository.UserPerceiveGradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@Transactional
public class UserPerceiveGradeManager {
    private final UserPerceiveGradeRepository userPerceiveGradeRepository;
    private final PerceiveGradeCalculator perceiveGradeCalculator;
    private final ClimbingGradeRepository climbingGradeRepository;
    private final UserAccountManager userAccountManager;

    public UserPerceiveGradeManager(UserPerceiveGradeRepository userPerceiveGradeRepository,
                                    PerceiveGradeCalculator perceiveGradeCalculator,
                                    ClimbingGradeRepository climbingGradeRepository,
                                    UserAccountManager userAccountManager){
        this.userPerceiveGradeRepository = userPerceiveGradeRepository;
        this.perceiveGradeCalculator = perceiveGradeCalculator;
        this.climbingGradeRepository = climbingGradeRepository;
        this.userAccountManager = userAccountManager;
    }

    public void addPerceiveGrade(ClimbingProblem problem, String firebaseUid, GradeDefinition gradeDefinition){
        UserAccount userAccount = getUserAccount(firebaseUid);
        ClimbingGrade grade = getClimbingGrade(gradeDefinition);

        UserPerceiveGrade perceiveGrade = new UserPerceiveGrade();
        perceiveGrade.setClimbingGrade(grade);
        perceiveGrade.setClimbingProblem(problem);
        perceiveGrade.setUserAccount(userAccount);

        userPerceiveGradeRepository.save(perceiveGrade);
    }

    public String getPerceiveGrade(ClimbingProblem problem){
        return perceiveGradeCalculator.findPerceiveGrade(problem);
    }

    private ClimbingGrade getClimbingGrade(GradeDefinition gradeDefinition){
        Optional<ClimbingGrade> grade = climbingGradeRepository.findByGradeDefinition(gradeDefinition);
        return grade.orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("The climbing grade %s is not on database. Please contact developers for help.",
                                gradeDefinition.name())
                )
        );
    }

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        if (userAccount == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Unknown authorization for user account. Please re-login and try again.");
        }
        return userAccount;
    }
}
