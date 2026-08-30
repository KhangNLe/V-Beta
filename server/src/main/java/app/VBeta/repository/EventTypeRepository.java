package app.VBeta.repository;

import app.VBeta.domain.model.notification.EventType;
import app.VBeta.domain.model.notification.EventTypeName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link EventType} catalog rows.
 */
public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    /**
     * Finds a seeded event type by name.
     *
     * @param eventTypeName event type enum
     * @return matching event type when present
     */
    Optional<EventType> findByEventTypeName(EventTypeName eventTypeName);
}
