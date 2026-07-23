package app.VBeta.domain.model.climb;

import jakarta.persistence.*;
import lombok.*;

/**
 * {@code ClimbingGrade} is a persisted grade reference used by climbing problems and perceived grades.
 */
@Entity
@Table(name = "Climbing_Grade")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClimbingGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long id;

    @Column(name = "grade", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private GradeDefinition gradeDefinition;
}
