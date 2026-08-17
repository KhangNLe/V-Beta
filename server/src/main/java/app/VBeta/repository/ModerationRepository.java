package app.VBeta.repository;

import app.VBeta.domain.model.moderation.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationRepository extends JpaRepository<ModerationAction, Long> {
}
