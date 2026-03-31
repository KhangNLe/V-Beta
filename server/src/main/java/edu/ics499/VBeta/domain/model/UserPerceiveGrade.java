package edu.ics499.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @MapsId("user_id")
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("grade_id")
    @JoinColumn(name = "grade_id", referencedColumnName = "grade_id")
    private ClimbingGrade climbingGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("problem_id")
    @JoinColumn(name = "problem_id", referencedColumnName = "problem_id")
    private ClimbingProblem climbingProblem;
}
