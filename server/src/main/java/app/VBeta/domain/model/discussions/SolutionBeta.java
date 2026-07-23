package app.VBeta.domain.model.discussions;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * {@code SolutionBeta} stores a user's uploaded beta media metadata.
 */
@Entity
@Table(name = "Solution_Beta")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionBeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beta_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", referencedColumnName = "discussion_id", unique = true, nullable = false)
    private DiscussionRoot discussionRoot;

    @Column(name = "beta_name", nullable = false, length = 250)
    private String betaName;

    @Column(name = "video_url", length = 250, nullable = false)
    private String videoURL;

    @Column(name = "create_date")
    private LocalDateTime createDate;
}
