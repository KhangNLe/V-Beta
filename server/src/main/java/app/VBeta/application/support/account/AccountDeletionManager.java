package app.VBeta.application.support.account;

import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.grade.UserPerceiveGradeManager;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@code AccountDeletionManager} coordinates full account teardown across dependent domains.
 * <p>
 * The deletion flow removes user-authored discussion comments, perceived grades, and
 * solution betas before deleting the owning {@link UserAccount}. Running these steps inside
 * one transaction ensures partial deletes are rolled back when a consistency check fails.
 */
@Service
@Transactional
public class AccountDeletionManager {
    private final DiscussionCommentManager discussionCommentManager;
    private final SolutionBetaManager solutionBetaManager;
    private final UserPerceiveGradeManager userPerceiveGradeManager;
    private final UserAccountManager userAccountManager;
    private final DiscussionRootManager discussionRootManager;

    /**
     * Constructs a new {@code AccountDeletionManager} with deletion collaborators.
     *
     * @param discussionCommentManager manager for user discussion-comment cleanup
     * @param solutionBetaManager manager for user beta/video cleanup
     * @param userPerceiveGradeManager manager for user perceived-grade cleanup
     * @param userAccountManager manager for deleting the user account row
     * @param discussionRootManager manager for discussion-root cleanup
     */
    public AccountDeletionManager(DiscussionCommentManager discussionCommentManager,
                                  SolutionBetaManager solutionBetaManager,
                                  UserPerceiveGradeManager userPerceiveGradeManager,
                                  UserAccountManager userAccountManager,
                                  DiscussionRootManager discussionRootManager){
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
        this.userPerceiveGradeManager = userPerceiveGradeManager;
        this.userAccountManager = userAccountManager;
        this.discussionRootManager = discussionRootManager;
    }

    /**
     * Deletes all user-owned discussion artifacts and finally the account itself.
     *
     * @param userAccount account targeted for teardown
     * @throws RuntimeException when related data integrity checks fail
     */
    public void deleteAllUserRelatedDiscussion(UserAccount userAccount){
        List<DiscussionRoot> userDiscussionComments = discussionRootManager.getUserDiscussionsByType(userAccount,
                DiscussionType.COMMENT);
        List<DiscussionRoot> userDiscussionBetas = discussionRootManager.getUserDiscussionsByType(userAccount,
                DiscussionType.BETA);

        discussionCommentManager.removeAllDiscussionRelatedComments(userDiscussionComments);
        userPerceiveGradeManager.removeAllUserRelatedPerceiveGrade(userAccount);
        solutionBetaManager.removeAllDiscussionRelatedSolutionBeta(userDiscussionBetas);

        userDiscussionComments.addAll(userDiscussionBetas);
        discussionRootManager.removeDiscussions(userDiscussionComments);
        userAccountManager.removeAccount(userAccount);
    }

}
