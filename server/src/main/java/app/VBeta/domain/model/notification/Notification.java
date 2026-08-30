package app.VBeta.domain.model.notification;

import app.VBeta.domain.model.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * {@code Notification} is a per-recipient inbox row pointing at a domain {@link Events} fact.
 * {@code readAt} is null until the recipient marks the notification read.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "event_id")
    private Events event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", referencedColumnName = "user_id")
    private UserAccount recipient;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    protected void onCreate(){
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
