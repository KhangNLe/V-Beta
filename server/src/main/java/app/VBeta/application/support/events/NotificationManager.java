package app.VBeta.application.support.events;

import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * {@code NotificationManager} persists and reads per-recipient inbox rows.
 */
@Service
@Transactional
public class NotificationManager {
    private final NotificationRepository notificationRepository;

    /**
     * Constructs a new {@code NotificationManager} with notification repository access.
     *
     * @param notificationRepository repository for notification entities
     */
    public NotificationManager(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Stores an unread inbox row for one recipient of an event.
     *
     * @param events event the notification points at
     * @param recipient inbox owner
     */
    public void pushNotification(Events events, UserAccount recipient) {
        notificationRepository.save(Notification.builder()
                .event(events)
                .recipient(recipient)
                .build()
        );
    }

    /**
     * Returns unread notifications for a recipient ({@code read_at} is null).
     *
     * @param user inbox owner
     * @return unread notification rows
     */
    public List<Notification> getUserUnreadNotifications(UserAccount user) {
        return notificationRepository.findAllUnreadByRecipientUser(user);
    }

    public Optional<Notification> findNotificationByIdAndOwner(Long id, UserAccount user) {
        return notificationRepository.findByNotificationIdAndRecipient(id, user);
    }

    public void save(Notification notification){
        notificationRepository.save(notification);
    }
}
