package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.DiscussionComment;
import edu.ics499.VBeta.domain.model.UserBeta;
import edu.ics499.VBeta.domain.model.SolutionBeta;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.repository.UserBetaRepository;
import edu.ics499.VBeta.repository.SolutionBetaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SolutionBetaManager {
    private final UserBetaRepository userBetaRepository;
    private final SolutionBetaRepository solutionBetaRepository;

    public SolutionBetaManager(UserBetaRepository userBetaRepository,
                               SolutionBetaRepository solutionBetaRepository){
        this.userBetaRepository = userBetaRepository;
        this.solutionBetaRepository = solutionBetaRepository;
    }

    public List<SolutionBeta> getProblemSolutionBeta(ClimbingProblem problem){
        List<UserBeta> userBetas = getUserBetasForClimbingProblem(problem);
        if (userBetas.isEmpty()) return null;
        List<SolutionBeta> betas = new ArrayList<>();
        userBetas.forEach(ub -> {
            Optional<SolutionBeta> beta = solutionBetaRepository.findByUserBeta(ub);
            beta.ifPresent(betas::add);
        });
        return betas;
    }

    public List<UserBeta> getUserBetasForClimbingProblem(ClimbingProblem problem){
        return userBetaRepository.findByProblem(problem);
    }

    public SolutionBeta getSolutionBetaFromUserBeta(UserBeta userBeta){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByUserBeta(userBeta);
        return solutionBeta.orElse(null);
    }
}
