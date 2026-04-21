package edu.ics499.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code UserPerceiveGrade} stores an individual user's selected grade for a climbing problem.
 */
@Entity
@Table(name = "User_Perceive_Grade")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPerceiveGrade {
    @EmbeddedId
    private UserPerceiveGradeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("problemId")
    @JoinColumn(name = "problem_id", referencedColumnName = "problem_id")
    private ClimbingProblem climbingProblem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", referencedColumnName = "grade_id", nullable = false)
    private ClimbingGrade climbingGrade;
}
