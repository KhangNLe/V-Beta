package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.domain.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ClimbingProblemDiscussionManager} composes a unified discussion timeline for a climbing problem.
 * It merges text comments and beta video submissions into a shared
 * {@link UserCommentData} stream ordered by creation time.
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
     *
     * @param problem climbing problem context
     * @return discussion timeline entries in database order
     */
    public List<UserCommentData> getCommentsForProblem(ClimbingProblem problem){
        List<DiscussionRoot> discussionRoots = discussionRootManager.getDiscussionForProblem(problem);
        List<UserCommentData> data = new ArrayList<>();

        discussionRoots.forEach(root -> {
            UserCommentData dataContent = null;
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
    public UserCommentData storeDiscussionComment(UserAccount user, ClimbingProblem problem, String commentInfo){
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

    private UserCommentData getCommentDiscussion(DiscussionRoot discussionRoot){
        DiscussionComment comment = discussionCommentManager.getDiscussionComment(discussionRoot);
        if (comment == null) return null;
        Long parentId = discussionRoot.getParent() == null ? null : discussionRoot.getParent().getDiscussionId();
        return new UserCommentData(
                discussionRoot.getDiscussionId(),
                discussionRoot.getUserAccount().getId(),
                discussionRoot.getUserAccount().getUsername(),
                parentId,
                discussionRoot.getDiscussionType(),
                comment.getCommentInfo(),
                comment.getCreateDate()
        );
    }

    private UserCommentData getSolutionBeta(DiscussionRoot discussionRoot){
        SolutionBeta beta = solutionBetaManager.getSolutionBetaFromDiscussionRoot(discussionRoot);
        if (beta == null) return null;
        Long parentId = discussionRoot.getParent() == null ? null : discussionRoot.getParent().getDiscussionId();
        return  new UserCommentData(
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
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Unable to reply to a discussion with id %d", discussionParentId)
            );
        }
        return parent;
    }
}
