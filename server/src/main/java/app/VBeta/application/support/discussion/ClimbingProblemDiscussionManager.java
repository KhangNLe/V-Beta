package app.VBeta.application.support.discussion;

import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionComment;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ClimbingProblemDiscussionManager} composes a unified discussion timeline for a climbing problem.
 * It merges text comments and beta video submissions into a shared
 * {@link UserDiscussionData} stream ordered by creation time, omitting roots whose
 * {@code deletedAt} is set.
 * <p>
 * Data is sourced from {@link DiscussionCommentManager} and {@link SolutionBetaManager}.
 */
@Service
public class ClimbingProblemDiscussionManager {
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;
    private final DiscussionRootManager discussionRootManager;


    /**
     * Constructs a new {@code ClimbingProblemDiscussionManager} with discussion and beta collaborators.
     *
     * @param discussionCommentManager manager for text comment retrieval and persistence
     * @param solutionBetaManager manager for beta lookup and persistence
     * @param discussionRootManager manager for discussion-root creation and lookup
     */
    public ClimbingProblemDiscussionManager(
            DiscussionCommentManager discussionCommentManager,
            SolutionBetaManager solutionBetaManager,
            DiscussionRootManager discussionRootManager) {
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
        this.discussionRootManager = discussionRootManager;
    }

    /**
     * Returns merged comment and beta entries for a climbing problem in chronological order.
     * Soft-deleted discussion roots ({@code deletedAt != null}) are excluded.
     *
     * @param problem climbing problem context
     * @return visible discussion timeline entries in database order
     */
    public List<UserDiscussionData> getCommentsForProblem(ClimbingProblem problem){
        List<DiscussionRoot> discussionRoots = discussionRootManager.getDiscussionForProblem(problem);
        List<UserDiscussionData> data = new ArrayList<>();

        discussionRoots.forEach(root -> {
            if (root.getDeletedAt() != null) {
                return;
            }
            UserDiscussionData dataContent = null;
            if (root.getDiscussionType().equals(DiscussionType.COMMENT)){
                dataContent = getCommentDiscussion(root);
            } else if (root.getDiscussionType().equals(DiscussionType.BETA)){
                dataContent = getSolutionBeta(root);
            }
            if (dataContent != null){
                data.add(dataContent);
            }
        });

        return data;
    }

    /**
     * Stores a user-authored discussion comment for a climbing problem.
     *
     * @param user author account
     * @param problem target climbing problem
     * @param commentInfo comment text content
     * @return created timeline entry including discussion id metadata
     */
    public UserDiscussionData storeDiscussionComment(UserAccount user, ClimbingProblem problem, String commentInfo){
        DiscussionRoot discussionRoot = discussionRootManager.createNewDiscussion(user, problem,
                DiscussionType.COMMENT);
        discussionCommentManager.storeDiscussionComment(discussionRoot, commentInfo);
        return getCommentDiscussion(discussionRoot);
    }

    /**
     * Stores a reply comment under an existing discussion node.
     *
     * @param user reply author account
     * @param problem target climbing problem
     * @param commentContent comment body text
     * @param discussionParentId parent discussion identifier
     */
    public void storeReplyComment(UserAccount user, ClimbingProblem problem, String commentContent,
                                  Long discussionParentId){
        DiscussionRoot parent = getDiscussionNode(discussionParentId);
        DiscussionRoot replyDiscussion = discussionRootManager.createReplyDiscussionRoot(user, problem,
                DiscussionType.COMMENT, parent);
        discussionCommentManager.storeDiscussionComment(replyDiscussion, commentContent);
    }

    /**
     * Stores a top-level solution beta entry for a climbing problem.
     *
     * @param userAccount beta author account
     * @param problem target climbing problem
     * @param objectName storage object key/name
     * @param publicUrl public media URL
     * @return persisted solution beta
     */
    public SolutionBeta storeSolutionBeta(UserAccount userAccount, ClimbingProblem problem, String objectName, String publicUrl){
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(userAccount, problem, DiscussionType.BETA);
        return solutionBetaManager.storeUserSolutionBeta(discussion, objectName, publicUrl);
    }

    /**
     * Stores a reply solution beta under an existing discussion node.
     *
     * @param userAccount beta author account
     * @param problem target climbing problem
     * @param objectName storage object key/name
     * @param publicUrl public media URL
     * @param discussionParentId parent discussion identifier
     * @return persisted reply solution beta
     */
    public SolutionBeta storeReplySolutionBeta(UserAccount userAccount, ClimbingProblem problem,
                                       String objectName, String publicUrl, Long discussionParentId){
        DiscussionRoot parent = getDiscussionNode(discussionParentId);
        DiscussionRoot reply = discussionRootManager.createReplyDiscussionRoot(userAccount, problem,
                DiscussionType.BETA, parent);

        return solutionBetaManager.storeUserSolutionBeta(reply, objectName, publicUrl);
    }

    /**
     * Deprecated hard-delete path. Comment hide is now {@link #softDeleteDiscussionRoot}.
     * Removes a discussion comment by discussion id.
     *
     * @param discussionId discussion identifier to remove
     * @param commentContent expected comment content for consistency check
     */
    public void removeUserComment(Long discussionId, String commentContent){
        DiscussionRoot discussion = getDiscussionNode(discussionId);
        discussionCommentManager.removeUserComment(discussion, commentContent);
        discussionRootManager.removeDiscussion(discussion);
    }

    /**
     * Deprecated hard-delete path. Beta hide is now {@link #softDeleteDiscussionRoot}.
     * Removes a solution beta by discussion id.
     *
     * @param discussionId discussion identifier to remove
     * @param publicUrl expected public URL for consistency check
     */
    public void removeUserSolutionBeta(Long discussionId, String publicUrl){
        DiscussionRoot discussion = getDiscussionNode(discussionId);
        solutionBetaManager.removeUserSolutionBeta(discussion, publicUrl);
        discussionRootManager.removeDiscussion(discussion);
    }

    /**
     * Marks a discussion root as deleted without removing the row or child comment/beta records.
     *
     * @param actor account performing the delete (stored as {@code deletedBy})
     * @param discussionRootId discussion identifier to soft-delete
     * @param deletedReason reason stored on the root (max 100 characters)
     * @throws RuntimeException when the discussion is missing or already deleted
     */
    public void softDeleteDiscussionRoot(UserAccount actor, Long discussionRootId, String deletedReason){
        DiscussionRoot discussionRoot = getDiscussionNode(discussionRootId);
        if (discussionRoot.getDeletedAt() != null) {
            throw new RuntimeException("Invalid action. Discussion is already deleted.");
        }
        discussionRoot.setDeletedAt(LocalDateTime.now());
        discussionRoot.setDeletedBy(actor);
        discussionRoot.setDeletedReason(deletedReason);
        discussionRootManager.updateDiscussionRoot(discussionRoot);
    }

    /**
     * Clears soft-delete metadata so a discussion root is visible on timelines again.
     * <p>
     * If {@code deletedAt} is already unset, the method returns without a second write.
     *
     * @param discussionRootId discussion identifier to restore
     * @throws RuntimeException when the discussion is missing
     */
    public void restoreDiscussionRoot(Long discussionRootId) {
        DiscussionRoot discussionRoot = getDiscussionNode(discussionRootId);
        if (discussionRoot.getDeletedAt() == null) {
            return;
        }
        discussionRoot.setDeletedAt(null);
        discussionRoot.setDeletedBy(null);
        discussionRoot.setDeletedReason(null);
        discussionRootManager.updateDiscussionRoot(discussionRoot);
    }

    /**
     * Maps a discussion root to a timeline DTO. Does not apply soft-delete filtering;
     * callers that build public timelines should skip roots with {@code deletedAt} set.
     *
     * @param discussionRoot discussion root to map
     * @return comment or beta timeline payload, or {@code null} when child content is missing
     */
    public UserDiscussionData getDiscussionData(DiscussionRoot discussionRoot){
        UserDiscussionData dataContent = null;
        if (discussionRoot.getDiscussionType().equals(DiscussionType.COMMENT)){
            dataContent = getCommentDiscussion(discussionRoot);
        } else {
            dataContent = getSolutionBeta(discussionRoot);
        }
        return dataContent;
    }

    private UserDiscussionData getCommentDiscussion(DiscussionRoot discussionRoot){
        DiscussionComment comment = discussionCommentManager.getDiscussionComment(discussionRoot);
        if (comment == null) return null;
        Long parentId = discussionRoot.getParent() == null ? null : discussionRoot.getParent().getDiscussionId();
        return new UserDiscussionData(
                discussionRoot.getDiscussionId(),
                discussionRoot.getUserAccount().getId(),
                discussionRoot.getUserAccount().getUsername(),
                parentId,
                discussionRoot.getDiscussionType(),
                comment.getCommentInfo(),
                comment.getCreateDate()
        );
    }

    private UserDiscussionData getSolutionBeta(DiscussionRoot discussionRoot){
        SolutionBeta beta = solutionBetaManager.getSolutionBetaFromDiscussionRoot(discussionRoot);
        if (beta == null) return null;
        Long parentId = discussionRoot.getParent() == null ? null : discussionRoot.getParent().getDiscussionId();
        return  new UserDiscussionData(
                discussionRoot.getDiscussionId(),
                discussionRoot.getUserAccount().getId(),
                discussionRoot.getUserAccount().getUsername(),
                parentId,
                discussionRoot.getDiscussionType(),
                beta.getVideoURL(),
                beta.getCreateDate()
        );
    }

    private DiscussionRoot getDiscussionNode(Long discussionParentId){
        DiscussionRoot parent = discussionRootManager.findDiscussionRootById(discussionParentId);
        if (parent == null){
            throw new RuntimeException(String.format("Unable to reply to a discussion with id %d", discussionParentId)
            );
        }
        return parent;
    }
}
