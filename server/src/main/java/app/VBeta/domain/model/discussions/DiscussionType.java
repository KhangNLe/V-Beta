package app.VBeta.domain.model.discussions;

/**
 * Enumerates discussion item kinds persisted in {@link DiscussionRoot}.
 * <p>
 * Discriminator for which detail tables are attached:
 * {@code COMMENT} → {@link DiscussionComment} only;
 * {@code BETA} → {@link SolutionBeta} only;
 * {@code MIXED} → both on the same root (one post with text and video).
 */
public enum DiscussionType {
    /** Text discussion comment entry. */
    COMMENT,
    /** Solution beta media entry. */
    BETA,
    /** Combined text comment and solution beta on one discussion root. */
    MIXED
}
