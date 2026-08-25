package app.VBeta.domain.model.appeal;

import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * {@code Appeal} is the one-time appeal path for a content owner after removal.
 * <p>
 * At most one appeal exists per {@link Report}. New rows default to {@link AppealStatus#OPEN}.
 */
@Entity
@Table(name = "Appeal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appeal_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appeal_user_id", referencedColumnName = "user_id")
    private UserAccount appealUser;

    @Column(name = "reason", nullable = false, length = 250)
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "appeal_status")
    private AppealStatus appealStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", referencedColumnName = "user_id")
    private UserAccount reviewedBy;

    @Column(name = "admin_note")
    private String adminNote;

    @PrePersist
    protected void onCreate() {
        if (appealStatus == null) {
            appealStatus = AppealStatus.OPEN;
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
