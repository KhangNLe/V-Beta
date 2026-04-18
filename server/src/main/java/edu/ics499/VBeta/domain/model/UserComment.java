package edu.ics499.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.*;


/**
 * {@code UserComment} links a user and climbing problem for discussion records.
 */
@Entity
@Table(name = "User_Comment")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_comment_id")
    private Long userCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", referencedColumnName = "problem_id")
    private ClimbingProblem climbingProblem;
}
