package app.VBeta.application.support.events;

import app.VBeta.domain.model.notification.EventTargetType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.report.Report;
import app.VBeta.repository.EventTypeRepository;
import app.VBeta.repository.EventsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Transactional
@Service
public class EventsManager {
    private final EventsRepository eventsRepository;
    private final EventTypeRepository eventTypeRepository;

    public EventsManager(EventsRepository eventsRepository, EventTypeRepository eventTypeRepository) {
        this.eventsRepository = eventsRepository;
        this.eventTypeRepository = eventTypeRepository;
    }

    public Events createReportEvent(Report report) {
        return eventsRepository.save(Events.builder()
                .eventType(
                        eventTypeRepository.findByEventTypeName(EventTypeName.REPORT_CREATED)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))
                )
                .actorUser(report.getReporter())
                .targetType(EventTargetType.REPORT)
                .report(report)
                .build()
        );
    }
}
