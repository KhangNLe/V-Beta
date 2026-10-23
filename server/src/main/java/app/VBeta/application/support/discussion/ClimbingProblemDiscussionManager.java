package app.VBeta.application.support.discussion;

import app.VBeta.api.dto.discussions.DiscussionContentReplyRequest;
import app.VBeta.api.dto.discussions.DiscussionContentRequest;
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
import java.util.Objects;

/**
 * {@code ClimbingProblemDiscussionManager} composes a unified discussion timeline for a climbing problem.
 * It maps {@code COMMENT}, {@code BETA}, and {@code MIXED} roots into
 * {@link UserDiscussionData} (nullable {@code comment} + {@code videoURL}) ordered by creation time,
 * omitting roots whose {@code deletedAt} is set.
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
     * Soft-deleted roots remain as placeholders ({@code isDeleted = true}) so reply threads
     * keep their parent anchors.
     *
     * @param problem climbing problem context
     * @return visible discussion timeline entries in database order
     */
    public List<UserDiscussionData> getCommentsForProblem(ClimbingProblem problem){
        List<DiscussionRoot> discussionRoots = discussionRootManager.getDiscussionForProblem(problem);
        List<UserDiscussionData> data = new ArrayList<>();

        discussionRoots.forEach(root -> {
            if (root == null) return;
            UserDiscussionData dataContent = getDiscussionData(root);
            if (dataContent == null) return;
            if (root.getDeletedAt() != null) {
                data.add(getSoftDeletedData(root));
            } else {
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
     * @param parentRoot parent discussion root
     * @return timeline DTO with comment set and {@code parentDiscussionId} pointing at the parent
     */
    public UserDiscussionData storeReplyComment(UserAccount user, ClimbingProblem problem, String commentContent,
                                  DiscussionRoot parentRoot){
        DiscussionRoot replyDiscussion = discussionRootManager.createReplyDiscussionRoot(user, problem,
                DiscussionType.COMMENT, parentRoot);
        discussionCommentManager.storeDiscussionComment(replyDiscussion, commentContent);
        return convertDiscussionData(replyDiscussion, commentContent, null);
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
     * @param parentRoot parent discussion identifier
     * @return persisted reply solution beta
     */
    public UserDiscussionData storeReplySolutionBeta(UserAccount userAccount, ClimbingProblem problem,
                                       String objectName, String publicUrl, DiscussionRoot parentRoot){
        DiscussionRoot reply = discussionRootManager.createReplyDiscussionRoot(userAccount, problem,
                DiscussionType.BETA, parentRoot);

        solutionBetaManager.storeUserSolutionBeta(reply, objectName, publicUrl);
        return convertDiscussionData(reply, null, publicUrl);
    }

    /**
     * Persists comment and beta children for a {@code MIXED} discussion root.
     *
     * @param root discussion root already typed as {@link DiscussionType#MIXED}
     * @param request create payload with text and uploaded video metadata
     * @return timeline DTO with both payloads set
     */
    public UserDiscussionData storeNewMixedDiscussion(DiscussionRoot root, DiscussionContentRequest request){
        discussionCommentManager.storeDiscussionComment(root, request.commentInfo());
        solutionBetaManager.storeUserSolutionBeta(root, request.objectFileName(), request.videoURL());
        return convertDiscussionData(root, request.commentInfo(), request.videoURL());
    }

    /**
     * Stores a mixed reply (comment text + beta video) under an existing discussion node.
     *
     * @param user reply author account
     * @param problem target climbing problem
     * @param request reply payload with comment and uploaded video metadata
     * @param parentRoot parent discussion root
     * @return timeline DTO with both payloads set and {@code parentDiscussionId} pointing at the parent
     */
    public UserDiscussionData storeReplyMixedDiscussion(UserAccount user, ClimbingProblem problem,
                                                        DiscussionContentReplyRequest request, DiscussionRoot parentRoot){
        DiscussionRoot replyRoot = discussionRootManager.createReplyDiscussionRoot(user, problem, DiscussionType.MIXED,
                parentRoot);
        discussionCommentManager.storeDiscussionComment(replyRoot, request.commentInfo());
        solutionBetaManager.storeUserSolutionBeta(replyRoot, request.objectFileName(), request.videoURL());
        return convertDiscussionData(replyRoot, request.commentInfo(), request.videoURL());
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
        } else if (discussionRoot.getDiscussionType().equals(DiscussionType.BETA)){
            dataContent = getSolutionBeta(discussionRoot);
        } else {
            dataContent = getMixedDiscussionContent(discussionRoot);
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
                comment.getCommentInfo(),
                null,
                comment.getCreateDate(),
                false
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
                null,
                beta.getVideoURL(),
                beta.getCreateDate(),
                false
        );
    }

    private UserDiscussionData getMixedDiscussionContent(DiscussionRoot root){
        DiscussionComment comment =  discussionCommentManager.getDiscussionComment(root);
        SolutionBeta beta = solutionBetaManager.getSolutionBetaFromDiscussionRoot(root);
        if (comment == null || beta == null) return null;
        Long parentId =  root.getParent() == null ? null : root.getParent().getDiscussionId();
        return new UserDiscussionData(
                root.getDiscussionId(),
                root.getUserAccount().getId(),
                root.getUserAccount().getUsername(),
                parentId,
                comment.getCommentInfo(),
                beta.getVideoURL(),
                root.getCreatedAt(),
                false
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

    private UserDiscussionData convertDiscussionData(DiscussionRoot root, String comment, String videoURL){
        Long parentId = root.getParent() == null ? null : root.getParent().getDiscussionId();
        return new UserDiscussionData(
                root.getDiscussionId(),
                root.getUserAccount().getId(),
                root.getUserAccount().getUsername(),
                parentId,
                comment,
                videoURL,
                root.getCreatedAt(),
                false
        );
    }

    private UserDiscussionData getSoftDeletedData(DiscussionRoot root){
        Long parentId = root.getParent() == null ? null : root.getParent().getDiscussionId();
        UserAccount author = root.getUserAccount();
        UserAccount deletedBy = root.getDeletedBy();
        String comment = Objects.equals(author, deletedBy)? "The discussion had been deleted by the author." :
                "The discussion had been deleted by one of the admin.";
        return new UserDiscussionData(
                root.getDiscussionId(),
                null,
                null,
                parentId,
                comment,
                null,
                root.getDeletedAt(),
                true
        );
    }
}
