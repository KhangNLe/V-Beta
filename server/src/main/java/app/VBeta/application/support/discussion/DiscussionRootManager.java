package app.VBeta.application.support.discussion;

import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionComment;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.DiscussionRootRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@code DiscussionRootManager} manages creation, lookup, and deletion of
 * {@link DiscussionRoot} records that anchor all problem-discussion items.
 * <p>
 * It provides query helpers used by discussion timeline composition and
 * authorization flows.
 */
@Service
@Transactional
public class DiscussionRootManager {
    private final DiscussionRootRepository discussionRootRepository;
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;

    /**
     * Constructs a new {@code DiscussionRootManager} with repository and related managers.
     *
     * @param discussionRootRepository repository for discussion-root persistence
     * @param discussionCommentManager manager for discussion-comment operations
     * @param solutionBetaManager manager for solution-beta operations
     */
    public DiscussionRootManager(DiscussionRootRepository discussionRootRepository,
                                 DiscussionCommentManager discussionCommentManager,
                                 SolutionBetaManager solutionBetaManager) {
        this.discussionRootRepository = discussionRootRepository;
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
    }

    /**
     * Creates a new top-level discussion root for a problem.
     *
     * @param author author account for the discussion
     * @param problem target climbing problem
     * @param discussionType discussion kind (comment/beta)
     * @return persisted discussion root
     */
    public DiscussionRoot createNewDiscussion(UserAccount author, ClimbingProblem problem,
                                              DiscussionType discussionType){
        DiscussionRoot discussionRoot = new DiscussionRoot();
        discussionRoot.setParent(null);
        discussionRoot.setProblem(problem);
        discussionRoot.setDiscussionType(discussionType);
        discussionRoot.setUserAccount(author);
        discussionRoot.setCreatedAt(LocalDateTime.now());

        return discussionRootRepository.save(discussionRoot);
    }

    /**
     * Creates a child discussion root under an existing parent node.
     *
     * @param account author account for the reply
     * @param problem target climbing problem
     * @param discussionType discussion kind (comment/beta)
     * @param parent parent discussion root
     * @return persisted child discussion root
     */
    public DiscussionRoot createSubDiscussionThread(UserAccount account, ClimbingProblem problem,
                                                    DiscussionType discussionType, DiscussionRoot parent){
        DiscussionRoot discussionRoot = createNewDiscussion(account, problem, discussionType);
        discussionRoot.setParent(parent);

        return discussionRootRepository.save(discussionRoot);
    }

    /**
     * Returns all discussion roots attached to a climbing problem in timeline order.
     *
     * @param problem climbing problem context
     * @return discussion roots ordered by create date ascending, then id ascending
     */
    public List<DiscussionRoot> getDiscussionForProblem(ClimbingProblem problem){
        return discussionRootRepository.findByProblem_IdOrderByCreatedAtAscDiscussionIdAsc(problem.getId());
    }

    /**
     * Creates a reply discussion root with an explicit parent.
     *
     * @param userAccount author account for the reply
     * @param problem target climbing problem
     * @param discussionType discussion kind (comment/beta)
     * @param parent parent discussion root
     * @return persisted reply discussion root
     */
    public DiscussionRoot createReplyDiscussionRoot(UserAccount userAccount, ClimbingProblem problem,
                                                  DiscussionType discussionType, DiscussionRoot parent){
        DiscussionRoot discussionRoot = new DiscussionRoot();
        discussionRoot.setParent(parent);
        discussionRoot.setUserAccount(userAccount);
        discussionRoot.setCreatedAt(LocalDateTime.now());
        discussionRoot.setProblem(problem);
        discussionRoot.setDiscussionType(discussionType);
        return discussionRootRepository.save(discussionRoot);
    }

    /**
     * Finds a discussion root by identifier.
     *
     * @param discussionRootId discussion root identifier
     * @return matching discussion root or {@code null} when missing
     */
    public DiscussionRoot findDiscussionRootById(Long discussionRootId){
        Optional<DiscussionRoot> discussionRoot = discussionRootRepository.findById(discussionRootId);
        return discussionRoot.orElse(null);
    }

    /**
     * Returns discussion roots authored by a user for a specific discussion type.
     *
     * @param userAccount author account
     * @param discussionType discussion kind filter
     * @return matching discussion roots
     */
    public List<DiscussionRoot> getUserDiscussionsByType(UserAccount userAccount, DiscussionType discussionType){
        return discussionRootRepository.findByUserAccount_AndDiscussionType(userAccount, discussionType);
    }

    /**
     * Returns discussion roots for a problem filtered by type.
     *
     * @param problem climbing problem context
     * @param discussionType discussion kind filter
     * @return matching discussion roots
     */
    public List<DiscussionRoot> getDiscussionsByProblemAndType(ClimbingProblem problem, DiscussionType discussionType){
        return discussionRootRepository.findByProblem_AndDiscussionType(problem, discussionType);
    }

    /**
     * Deletes a single discussion root.
     *
     * @param discussionRoot discussion root to delete
     */
    public void removeDiscussion(DiscussionRoot discussionRoot){
        discussionRootRepository.delete(discussionRoot);
    }

    /**
     * Deletes a batch of discussion roots.
     *
     * @param discussionRoots discussion roots to delete
     */
    public void removeDiscussions(List<DiscussionRoot> discussionRoots){
        discussionRootRepository.deleteAll(discussionRoots);
    }

    /**
     * Finds discussion roots authored by a user on a specific problem.
     *
     * @param userAccount author account
     * @param problem climbing problem context
     * @return matching discussion roots
     */
    public List<DiscussionRoot> findDiscussionRootByUserAndProblem(UserAccount userAccount, ClimbingProblem problem){
        return discussionRootRepository.findByUserAccount_AndProblem(userAccount, problem);
    }

    public void updateDiscussionRoot(DiscussionRoot discussionRoot){
        discussionRootRepository.save(discussionRoot);
    }

    public boolean validateDiscussionCommentContent(DiscussionRoot discussionRoot, String commentContent){
        if (!discussionRoot.getDiscussionType().equals(DiscussionType.COMMENT)){
            throw new RuntimeException("Mismatching discussion information");
        }
        DiscussionComment comment = discussionCommentManager.getDiscussionComment(discussionRoot);
        return comment.getCommentInfo().equals(commentContent);
    }

    public boolean validateDiscussionBetaContent(DiscussionRoot discussionRoot, String betaUrl){
        if (!discussionRoot.getDiscussionType().equals(DiscussionType.BETA)){
            throw new RuntimeException("Mismatching discussion information");
        }
        SolutionBeta beta = solutionBetaManager.getSolutionBetaFromDiscussionRoot(discussionRoot);
        return beta.getVideoURL().equals(betaUrl);
    }
}
