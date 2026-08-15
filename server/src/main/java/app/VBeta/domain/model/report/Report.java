package app.VBeta.domain.model.report;

import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate. annotations.JdbcTypeCode;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.SqlTypes;
import org.springframework.beans.factory.parsing.Problem;

import java.time.LocalDateTime;

@Entity
@Table(name = "Report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", referencedColumnName = "user_id")
    private UserAccount reporter;

    @Column(name = "report_reason", nullable = false, length = 250)
    private String reportReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "report_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReportStatus reportStatus;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", referencedColumnName = "discussion_id")
    private DiscussionRoot discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", referencedColumnName = "problem_id")
    private ClimbingProblem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wall_section_id", referencedColumnName = "wall_section_id")
    private WallSection wallSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserAccount user;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reportStatus == null) {
            reportStatus = ReportStatus.OPEN;
        }
    }
}
