package app.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * {@code ClimbingProblem} models a route/problem set on a wall section.
 */
@Entity
@Table(name = "Climbing_Problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ClimbingProblem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "problem_id")
    private Long id;

    @Column(name = "hold_color", nullable = false, length = 25)
    private String holdColor;

    @Column(name = "info", length = 250)
    private String problemInfo;

    @Column(name = "create_date")
    private LocalDateTime createdDate;

    @Column(name = "lifecycle_status")
    @Enumerated(EnumType.STRING)
    private LifecycleStatus problemStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wall_section_id", referencedColumnName = "wall_section_id")
    private WallSection wallSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_grade_id", referencedColumnName = "grade_id")
    private ClimbingGrade climbingGrade;

}
