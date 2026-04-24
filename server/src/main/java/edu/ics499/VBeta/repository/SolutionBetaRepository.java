package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.SolutionBeta;
import edu.ics499.VBeta.domain.model.UserBeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SolutionBeta} entities.
 */
public interface SolutionBetaRepository extends JpaRepository<SolutionBeta, Long>{
    /**
     * Finds a solution beta by its user-beta association row.
     *
     * @param userBeta user-beta association
     * @return matching solution beta when present
     */
    Optional<SolutionBeta> findByUserBeta(UserBeta userBeta);

    /**
     * Finds a solution beta by public video URL.
     *
     * @param publicUrl public video URL
     * @return matching solution beta when present
     */
    Optional<SolutionBeta> findByVideoURL(String publicUrl);

    /**
     * Finds a solution beta by candidate user-beta rows and video URL.
     *
     * @param userBetas candidate user-beta rows
     * @param videoUrl target video URL
     * @return matching solution beta when present
     */
    Optional<SolutionBeta> findByUserBetaInAndVideoURL(List<UserBeta> userBetas, String videoUrl);

    /**
     * Finds solution betas for a batch of user-beta association rows.
     *
     * @param userBetas user-beta association rows
     * @return matching solution beta rows
     */
    List<SolutionBeta> findByUserBetaIn(List<UserBeta> userBetas);
}
