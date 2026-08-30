package app.VBeta.application.support.events;

import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.notification.EventTargetType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.EventTypeRepository;
import app.VBeta.repository.EventsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code EventsManager} persists domain event rows used by in-app notifications.
 * <p>
 * Report-lifecycle events target a {@link Report} ({@code target_type = REPORT}).
 * Create uses the reporter as actor; appeal submit uses the content owner;
 * queue resolve uses the deciding admin.
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

    /**
     * Creates an {@code APPEAL_SUBMITTED} event for the given report.
     * <p>
     * The actor is the content owner and the event target is the report itself.
     *
     * @param report persisted report whose removal is being appealed
     * @param appealUser content owner who submitted the appeal
     * @return saved event
     * @throws RuntimeException when the {@code APPEAL_SUBMITTED} event type is missing
     */
    public Events createAppealSubmittedEvent(Report report, UserAccount appealUser) {
        return eventsRepository.save(Events.builder()
                .eventType(
                        eventTypeRepository.findByEventTypeName(EventTypeName.APPEAL_SUBMITTED)
                                .orElseThrow(() -> new RuntimeException("APPEAL_SUBMITTED event type is missing"))
                )
                .actorUser(appealUser)
                .targetType(EventTargetType.REPORT)
                .report(report)
                .build()
        );
    }

    /**
     * Creates a queue-resolve event for the given report.
     * <p>
     * The actor is the deciding admin and the event target is the report itself.
     * Used for {@code REPORT_DISMISSED}, {@code REPORT_APPROVED}, and
     * {@code CONTENT_REMOVED}.
     *
     * @param report persisted report
     * @param eventTypeName seeded event kind
     * @param decision logbook row supplying the admin actor
     * @return saved event
     * @throws IllegalArgumentException when the event type is missing
     */
    public Events createModeratedReportEvent(Report report, EventTypeName eventTypeName, ModerationAction decision) {
        return eventsRepository.save(Events.builder()
                .eventType(
                        eventTypeRepository.findByEventTypeName(eventTypeName)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("%s event type does not exist", eventTypeName)))
                )
                .actorUser(decision.getAdminUser())
                .targetType(EventTargetType.REPORT)
                .report(report)
                .build()
        );
    }
}
