package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.SolutionBeta;
import edu.ics499.VBeta.domain.model.UserBeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolutionBetaRepository extends JpaRepository<SolutionBeta, Long>{
    Optional<SolutionBeta> findByUserBeta(UserBeta userBeta);
}
