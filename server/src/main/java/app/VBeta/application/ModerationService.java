package app.VBeta.application;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.moderation.ModerationManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class ModerationService {
    private final ModerationManager moderationManager;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final UserAccountManager userAccountManager;
    private final ReportManager reportManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    public ModerationService(ModerationManager moderationManager,
                             AuthorizationService authorizationService,
                             NotificationService notificationService,
                             UserAccountManager userAccountManager,
                             ReportManager reportManager,
                             ClimbingProblemDiscussionManager climbingProblemDiscussionManager) {
        this.moderationManager = moderationManager;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.userAccountManager = userAccountManager;
        this.reportManager = reportManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
    }

    public void createModerationForReportQueue(ModerationRequest moderationRequest, String firebaseUid) {
        UserAccount admin = userAccountManager.findUserAccount(firebaseUid);
        if (admin == null){
            throw new RuntimeException("User not found");
        }
        authorizationService.authorize(admin, ActionDefinition.MODERATE_REPORT);
        validateQueueDecision(moderationRequest.decision());

        Set<Long> removedDiscussionIds = new HashSet<>();
        for (Long id : moderationRequest.reportIds()){
            Report report;
            try {
                report = reportManager.findOpenReportNotOfAdmin(admin, id);
            } catch (RuntimeException e) {
                continue;
            }
            if (report.getTargetType() != ReportTargetType.DISCUSSION) {
                continue;
            }
            createModerationForReport(moderationRequest, admin, report, removedDiscussionIds);
        }
    }

    private void validateQueueDecision(ModerateActionType decision) {
        if (decision != ModerateActionType.REPORT_DISMISSED
                && decision != ModerateActionType.CONTENT_REMOVED) {
            throw new RuntimeException("Appeal decisions are not supported on this endpoint.");
        }
    }

    private void createModerationForReport(ModerationRequest request, UserAccount admin, Report report,
                                           Set<Long> removedDiscussionIds) {
        ModerationAction decision = moderationManager.createModeration(report, admin, request);
        moderateDiscussionReport(decision, report, admin, removedDiscussionIds);
    }

    private void moderateDiscussionReport(ModerationAction decision, Report report, UserAccount admin,
                                          Set<Long> removedDiscussionIds) {
        switch (decision.getModerateActionType()) {
            case CONTENT_REMOVED -> {
                reportManager.updateReportStatus(report, ReportStatus.CONTENT_REMOVED);
                notificationService.sendReportModerationNotification(decision, report.getReporter(), report,
                        EventTypeName.REPORT_APPROVED);
                removeDiscussion(admin, report, decision, removedDiscussionIds);
            }
            case REPORT_DISMISSED -> {
                reportManager.updateReportStatus(report, ReportStatus.DISMISSED);
                notificationService.sendReportModerationNotification(decision, report.getReporter(), report,
                        EventTypeName.REPORT_DISMISSED);
            }
            default -> throw new RuntimeException("Appeal decisions are not supported on this endpoint.");
        }
    }

    private void removeDiscussion(UserAccount admin, Report report, ModerationAction decision,
                                  Set<Long> removedDiscussionIds) {
        Long discussionId = report.getDiscussion().getDiscussionId();
        if (!removedDiscussionIds.add(discussionId)) {
            return;
        }
        if (report.getDiscussion().getDeletedAt() != null) {
            return;
        }
        String removedReason = String.format("Admin approved the content deletion due to report of %s.",
                report.getCategory().getCategoryName());
        climbingProblemDiscussionManager.softDeleteDiscussionRoot(admin, discussionId, removedReason);
        notificationService.sendReportModerationNotification(decision, report.getDiscussion().getUserAccount(),
                report, EventTypeName.CONTENT_REMOVED);
    }
}
