package app.VBeta.repository;

import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.SolutionBeta;
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
     * @param discussionRoot candidate discussion root anchors
     * @return matching solution beta when present
     */
    Optional<SolutionBeta> findByDiscussionRoot(DiscussionRoot discussionRoot);

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
     * @param videoUrl target video URL
     * @param discussionRoot candidate discussion root anchors
     * @return matching solution beta when present
     */
    Optional<SolutionBeta> findByDiscussionRootAndVideoURL(DiscussionRoot discussionRoot, String videoUrl);

    /**
     * Finds solution betas for a batch of user-beta association rows.
     *
     * @param discussionRoots candidate discussion root anchors
     * @return matching solution beta rows
     */
    List<SolutionBeta> findByDiscussionRootIn(List<DiscussionRoot> discussionRoots);
}
