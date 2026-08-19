package app.VBeta.repository;

import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.domain.model.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Notification} entities.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Returns unread inbox rows for a recipient ({@code readAt} is null).
     *
     * @param user recipient account
     * @return unread notifications
     */
    @Query(
            "SELECT noti FROM Notification noti WHERE noti.recipient = :user AND noti.readAt IS NULL"
    )
    List<Notification> findAllUnreadByRecipientUser(@Param("user") UserAccount user);

    Optional<Notification> findByNotificationIdAndRecipient(Long id, UserAccount user);
}
