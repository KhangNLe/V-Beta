package app.VBeta.repository;

import app.VBeta.domain.model.moderation.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link ModerationAction} logbook rows.
 */
public interface ModerationRepository extends JpaRepository<ModerationAction, Long> {
}
