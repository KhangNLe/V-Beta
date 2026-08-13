package app.VBeta.application;

import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.events.EventsManager;
import app.VBeta.application.support.events.NotificationManager;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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

    public void sendReportNotification(Report report) {
        Events event = eventManager.createReportEvent(report);
        for (UserAccount admin : userAccountManager.findUsersOfRole(RoleType.ADMIN)) {
            if (Objects.equals(admin.getId(), report.getReporter().getId())) {
                continue;
            }
            notificationManager.pushNotification(event, admin);
        }
    }
}
