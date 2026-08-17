package app.VBeta.application;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.moderation.ModerationManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

@Service
@Transactional
public class ModerationService {
    private final ModerationManager moderationManager;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final UserAccountManager userAccountManager;
    private final ReportManager reportManager;
    private final DiscussionRootManager discussionRootManager;

    public ModerationService(ModerationManager moderationManager,
                             AuthorizationService authorizationService,
                             NotificationService notificationService,
                             UserAccountManager userAccountManager,
                             ReportManager reportManager,
                             DiscussionRootManager discussionRootManager) {
        this.moderationManager = moderationManager;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.userAccountManager = userAccountManager;
        this.reportManager = reportManager;
        this.discussionRootManager = discussionRootManager;
    }

    public void createModeration(ModerationRequest moderationRequest, String firebaseUid) {
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null){
            throw new RuntimeException("User not found");
        }
        authorizationService.authorize(user, ActionDefinition.MODERATE_REPORT);
        Report report = reportManager.findReportNotFromUser(user, moderationRequest.reportId());
        ModerationAction decision = moderationManager.createModeration(report, user, moderationRequest);

    }
}
