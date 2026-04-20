package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.WallSection;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link WallSection} entities.
 */
public interface WallSectionRepository extends JpaRepository<WallSection, Long> {
}