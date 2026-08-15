package app.VBeta.application.support.events;

import app.VBeta.domain.model.notification.EventTargetType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.report.Report;
import app.VBeta.repository.EventTypeRepository;
import app.VBeta.repository.EventsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code EventsManager} persists domain event rows used by in-app notifications.
 * <p>
 * Moderation notifications typically target a {@link Report} with
 * {@link EventTypeName#REPORT_CREATED}.
 */
@Transactional
@Service
public class EventsManager {
    private final EventsRepository eventsRepository;
    private final EventTypeRepository eventTypeRepository;

    /**
     * Constructs a new {@code EventsManager} with event repositories.
     *
     * @param eventsRepository repository for event entities
     * @param eventTypeRepository repository for seeded event-type lookups
     */
    public EventsManager(EventsRepository eventsRepository, EventTypeRepository eventTypeRepository) {
        this.eventsRepository = eventsRepository;
        this.eventTypeRepository = eventTypeRepository;
    }

    /**
     * Creates a {@code REPORT_CREATED} event for the given report.
     * <p>
     * The actor is the reporter and the event target is the report itself.
     *
     * @param report persisted report
     * @return saved event
     * @throws RuntimeException
     *         when the {@code REPORT_CREATED} event type is missing
     */
    public Events createReportEvent(Report report) {
        return eventsRepository.save(Events.builder()
                .eventType(
                        eventTypeRepository.findByEventTypeName(EventTypeName.REPORT_CREATED)
                                .orElseThrow(() -> new RuntimeException("REPORT_CREATED event type is missing"))
                )
                .actorUser(report.getReporter())
                .targetType(EventTargetType.REPORT)
                .report(report)
                .build()
        );
    }
}
