package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Constructs a new {@code AccountDeletionManager} with deletion collaborators.
     *
     * @param discussionCommentManager manager for user discussion-comment cleanup
     * @param solutionBetaManager manager for user beta/video cleanup
     * @param userPerceiveGradeManager manager for user perceived-grade cleanup
     * @param userAccountManager manager for deleting the user account row
     */
    public AccountDeletionManager(DiscussionCommentManager discussionCommentManager,
                                  SolutionBetaManager solutionBetaManager,
                                  UserPerceiveGradeManager userPerceiveGradeManager,
                                  UserAccountManager userAccountManager){
        this.discussionCommentManager = discussionCommentManager;
        this.solutionBetaManager = solutionBetaManager;
        this.userPerceiveGradeManager = userPerceiveGradeManager;
        this.userAccountManager = userAccountManager;
    }

    /**
     * Deletes all user-owned discussion artifacts and finally the account itself.
     *
     * @param userAccount account targeted for teardown
     * @throws org.springframework.web.server.ResponseStatusException when related data integrity checks fail
     */
    public void deleteAllUserRelatedDiscussion(UserAccount userAccount){
        discussionCommentManager.removeAllUserComments(userAccount);
        userPerceiveGradeManager.removeAllUserRelatedPerceiveGrade(userAccount);
        solutionBetaManager.removeAllUserRelatedSolutionBeta(userAccount);
        userAccountManager.removeAccount(userAccount);
    }

}
