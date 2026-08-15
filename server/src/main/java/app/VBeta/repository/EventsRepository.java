package app.VBeta.repository;

import app.VBeta.domain.model.notification.Events;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Events} entities.
 */
public interface EventsRepository extends JpaRepository<Events, Long>{
}
