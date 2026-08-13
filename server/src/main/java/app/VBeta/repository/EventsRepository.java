package app.VBeta.repository;

import app.VBeta.domain.model.notification.Events;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventsRepository extends JpaRepository<Events, Long>{
}
