package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.repository.UserPerceiveGradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * {@code UserPerceiveGradeManager} handles write and read workflows for user-perceived grades.
 * It ensures account and grade entities are valid before persisting a
 * {@link UserPerceiveGrade} record.
 * <p>
 * Aggregate/consensus output is delegated to {@link PerceiveGradeCalculator}.
 */
@Service
@Transactional
public class UserPerceiveGradeManager {
    private final UserPerceiveGradeRepository userPerceiveGradeRepository;
    private final PerceiveGradeCalculator perceiveGradeCalculator;
    private final ClimbingGradeRepository climbingGradeRepository;
    private final UserAccountManager userAccountManager;

    /**
     * Constructs a new {@code UserPerceiveGradeManager} with repositories and calculator dependencies.
     *
     * @param userPerceiveGradeRepository repository for perceived grade records
     * @param perceiveGradeCalculator calculator for consensus grade output
     * @param climbingGradeRepository repository for climbing grade definitions
     * @param userAccountManager manager for user account validation
     */
    public UserPerceiveGradeManager(UserPerceiveGradeRepository userPerceiveGradeRepository,
                                    PerceiveGradeCalculator perceiveGradeCalculator,
                                    ClimbingGradeRepository climbingGradeRepository,
                                    UserAccountManager userAccountManager){
        this.userPerceiveGradeRepository = userPerceiveGradeRepository;
        this.perceiveGradeCalculator = perceiveGradeCalculator;
        this.climbingGradeRepository = climbingGradeRepository;
        this.userAccountManager = userAccountManager;
    }

    /**
     * Creates or updates a user's perceived grade entry for a climbing problem.
     *
     * @param problem climbing problem being graded
     * @param firebaseUid Firebase UID of the submitting user
     * @param gradeDefinition selected grade definition
     */
    public void addPerceiveGrade(ClimbingProblem problem, String firebaseUid, GradeDefinition gradeDefinition){
        UserAccount userAccount = getUserAccount(firebaseUid);
        ClimbingGrade grade = getClimbingGrade(gradeDefinition);

        Optional<UserPerceiveGrade> existing =
                userPerceiveGradeRepository.findByUserAccountAndClimbingProblem(userAccount, problem);
        if (existing.isPresent()) {
            UserPerceiveGrade perceiveGrade = existing.get();
            perceiveGrade.setClimbingGrade(grade);
            userPerceiveGradeRepository.save(perceiveGrade);
            return;
        }

        UserPerceiveGrade perceiveGrade = new UserPerceiveGrade();
        UserPerceiveGradeId id = new UserPerceiveGradeId(userAccount.getId(), problem.getId());
        perceiveGrade.setId(id);
        perceiveGrade.setUserAccount(userAccount);
        perceiveGrade.setClimbingProblem(problem);
        perceiveGrade.setClimbingGrade(grade);
        userPerceiveGradeRepository.save(perceiveGrade);
    }

    /**
     * Returns the consensus perceived grade text for a climbing problem.
     *
     * @param problem climbing problem being evaluated
     * @return perceived grade label
     */
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
