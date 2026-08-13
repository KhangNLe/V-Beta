package app.VBeta.application.support.events;

import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class NotificationManager {
    private final NotificationRepository notificationRepository;

    public NotificationManager(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void pushNotification(Events events, UserAccount recipient) {
        notificationRepository.save(Notification.builder()
                .event(events)
                .recipient(recipient)
                .build()
        );
    }
}
