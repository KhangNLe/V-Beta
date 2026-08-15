package app.VBeta.application;

import app.VBeta.api.dto.notification.EventTypeDTO;
import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.events.EventsManager;
import app.VBeta.application.support.events.NotificationManager;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code NotificationService} is the orchestration layer for in-app moderation notifications.
 * <p>
 * It records {@code REPORT_CREATED} events and fans out inbox rows to admin recipients,
 * and maps unread notifications into lightweight DTOs for the client.
 */
@Service
@Transactional
public class NotificationService {
    private final NotificationManager notificationManager;
    private final EventsManager eventManager;
    private final UserAccountManager userAccountManager;

    /**
     * Constructs a new {@code NotificationService} with required collaborators.
     *
     * @param notificationManager manager for inbox persistence and unread reads
     * @param eventManager manager for event persistence
     * @param userAccountManager manager for recipient account lookups
     */
    public NotificationService(NotificationManager notificationManager,
                               EventsManager eventManager,
                               UserAccountManager userAccountManager) {
        this.notificationManager = notificationManager;
        this.eventManager = eventManager;
        this.userAccountManager = userAccountManager;
    }

    /**
     * Records a {@code REPORT_CREATED} event and notifies admins, skipping the reporter.
     *
     * @param report persisted report that triggered the event
     */
    public void saveReportNotification (Report report) {
        Events event = eventManager.createReportEvent(report);
        for (UserAccount admin : userAccountManager.findUsersOfRole(RoleType.ADMIN)) {
            if (Objects.equals(admin.getId(), report.getReporter().getId())) {
                continue;
            }
            notificationManager.pushNotification(event, admin);
        }
    }

    /**
     * Returns unread notification summaries for a user identified by Firebase UID.
     *
     * @param firebaseUid Firebase UID of the inbox owner
     * @return unread notification DTOs
     * @throws RuntimeException when no account matches the UID
     */
    public List<QuickNotificationDTO> getQuickNotifications(String firebaseUid) {
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new RuntimeException("User is not found");
        }
        List<Notification> notifications = notificationManager.getUserUnreadNotifications(user);

        return notifications.stream().map(this::createQuickNotificationDTO).toList();
    }

    /**
     * Maps a persisted notification into the short inbox DTO.
     *
     * @param notification unread notification row
     * @return event type and created-at summary
     */
    private QuickNotificationDTO createQuickNotificationDTO(Notification notification) {
        return new QuickNotificationDTO(
                createEventTypeDTO(notification.getEvent()),
                notification.getCreatedAt()
        );
    }

    /**
     * Maps an event's catalog type into the API DTO.
     *
     * @param event persisted event
     * @return event type name and description
     */
    private EventTypeDTO createEventTypeDTO(Events event) {
        return new EventTypeDTO(
                event.getEventType().getEventTypeName().name(),
                event.getEventType().getDescription()
        );
    }
}
