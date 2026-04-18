package edu.ics499.VBeta.domain.model;

import edu.ics499.VBeta.api.dto.UserCommentData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * {@code DiscussionComment} stores the text and timestamp for a user discussion entry.
 */
@Entity
@Table(name = "Discussion_Comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discussion_id")
    private Long id;

    @Column(name = "info", length = 250)
    private String commentInfo;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @OneToOne
    @JoinColumn(name = "user_comment_id", referencedColumnName = "user_comment_id", nullable = false, unique = true)
    private UserComment userComment;
}
