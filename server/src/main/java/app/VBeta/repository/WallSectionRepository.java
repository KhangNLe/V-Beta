package app.VBeta.repository;

import app.VBeta.domain.model.climb.WallSection;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link WallSection} entities.
 */
public interface WallSectionRepository extends JpaRepository<WallSection, Long> {
}