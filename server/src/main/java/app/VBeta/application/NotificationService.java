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

@Service
@Transactional
public class NotificationService {
    private final NotificationManager notificationManager;
    private final EventsManager eventManager;
    private final UserAccountManager userAccountManager;

    public NotificationService(NotificationManager notificationManager,
                               EventsManager eventManager,
                               UserAccountManager userAccountManager) {
        this.notificationManager = notificationManager;
        this.eventManager = eventManager;
        this.userAccountManager = userAccountManager;
    }

    public void saveReportNotification (Report report) {
        Events event = eventManager.createReportEvent(report);
        for (UserAccount admin : userAccountManager.findUsersOfRole(RoleType.ADMIN)) {
            if (Objects.equals(admin.getId(), report.getReporter().getId())) {
                continue;
            }
            notificationManager.pushNotification(event, admin);
        }
    }

    public List<QuickNotificationDTO> getQuickNotifications(String firebaseUid) {
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new RuntimeException("User is not found");
        }
        List<Notification> notifications = notificationManager.getUserUnreadNotifications(user);

        return notifications.stream().map(this::createQuickNotificationDTO).toList();
    }

    private QuickNotificationDTO createQuickNotificationDTO(Notification notification) {
        return new QuickNotificationDTO(
                createEventTypeDTO(notification.getEvent()),
                notification.getCreatedAt()
        );
    }

    private EventTypeDTO createEventTypeDTO(Events event) {
        return new EventTypeDTO(
                event.getEventType().getEventTypeName().name(),
                event.getEventType().getDescription()
        );
    }
}
