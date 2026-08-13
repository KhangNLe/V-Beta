package app.VBeta.repository;

import app.VBeta.domain.model.notification.EventType;
import app.VBeta.domain.model.notification.EventTypeName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    Optional<EventType> findByEventTypeName(EventTypeName eventTypeName);
}
