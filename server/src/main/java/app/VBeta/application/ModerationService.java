package app.VBeta.application;

import app.VBeta.api.dto.moderation.ModerationDTO;
import app.VBeta.api.dto.moderation.ModerationPayload;
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

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * {@code ModerationService} is the orchestration layer for admin report-queue
 * decisions ({@code POST /api/moderate/report}).
 * <p>
 * It authorizes {@link ActionDefinition#MODERATE_REPORT}, writes a
 * {@link ModerationAction} logbook row per eligible report, closes that reporter
 * row, and notifies the reporter. {@code CONTENT_REMOVED} also soft-deletes the
 * shared discussion once and notifies the owner once. Appeals are rejected.
 */
@Service
@Transactional
public class ModerationService {
    private final ModerationManager moderationManager;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final UserAccountManager userAccountManager;
    private final ReportManager reportManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final ReportService reportService;

    /**
     * Constructs a new {@code ModerationService} with required collaborators.
     *
     * @param moderationManager manager for append-only logbook rows
     * @param authorizationService service for {@code MODERATE_REPORT} checks
     * @param notificationService service for reporter/owner outcome inbox writes
     * @param userAccountManager manager for admin account lookups
     * @param reportManager manager for open-report lookup and status updates
     * @param climbingProblemDiscussionManager manager for discussion soft-delete
     */
    public ModerationService(ModerationManager moderationManager,
                             AuthorizationService authorizationService,
                             NotificationService notificationService,
                             UserAccountManager userAccountManager,
                             ReportManager reportManager,
                             ClimbingProblemDiscussionManager climbingProblemDiscussionManager, ReportService reportService) {
        this.moderationManager = moderationManager;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.userAccountManager = userAccountManager;
        this.reportManager = reportManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.reportService = reportService;
    }

    /**
     * Applies one queue decision to each eligible id in {@code reportIds}.
     * <p>
     * Allowed decisions are {@link ModerateActionType#REPORT_DISMISSED} and
     * {@link ModerateActionType#CONTENT_REMOVED}. Each id is resolved independently:
     * unknown, already-closed, admin-filed, admin-owned-discussion, and
     * non-{@code DISCUSSION} reports are skipped. Sibling OPEN reports not in
     * {@code reportIds} stay open.
     *
     * @param moderationRequest report ids, decision, and required admin notes
     * @param firebaseUid Firebase UID of the acting admin
     * @throws RuntimeException when the account is missing, unauthorized, or
     *         the decision is an appeal type
     */
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

    public ModerationPayload getModerationLog(String firebaseUid, Long moderationId){
        UserAccount admin = userAccountManager.findUserAccount(firebaseUid);
        if (admin == null){
            throw new RuntimeException("User not found");
        }

        authorizationService.authorize(admin, ActionDefinition.VIEW_MODERATION_LOGS);
        ModerationAction decision = moderationManager.findById(moderationId)
                .orElseThrow(() -> new RuntimeException("Moderation not found"));
        return createModerationPayload(List.of(decision));
    }

    public ModerationPayload getLogbook(String firebaseUid, int offSetPlace){
        UserAccount admin = userAccountManager.findUserAccount(firebaseUid);
        if (admin == null){
            throw new RuntimeException("User not found");
        }
        authorizationService.authorize(admin, ActionDefinition.VIEW_MODERATION_LOGS);
        List<ModerationAction> decisions = moderationManager.findLogsByOffset(offSetPlace);
        return createModerationPayload(decisions);
    }

    /**
     * Rejects appeal decisions; this endpoint only dismisses or removes content.
     *
     * @param decision requested logbook action type
     * @throws RuntimeException when the decision is not a queue resolve action
     */
    private void validateQueueDecision(ModerateActionType decision) {
        if (decision != ModerateActionType.REPORT_DISMISSED
                && decision != ModerateActionType.CONTENT_REMOVED) {
            throw new RuntimeException("Appeal decisions are not supported on this endpoint.");
        }
    }

    /**
     * Writes the logbook row for one report, then applies status and notification
     * side effects.
     *
     * @param request queue decision payload (notes are copied onto the logbook)
     * @param admin acting admin (logbook actor and event actor)
     * @param report OPEN discussion report visible to {@code admin}
     * @param removedDiscussionIds discussions already soft-deleted in this request
     */
    private void createModerationForReport(ModerationRequest request, UserAccount admin, Report report,
                                           Set<Long> removedDiscussionIds) {
        ModerationAction decision = moderationManager.createModeration(report, admin, request);
        moderateDiscussionReport(decision, report, admin, removedDiscussionIds);
    }

    /**
     * Closes the report and notifies the reporter. Dismiss does not notify the
     * owner. Remove also soft-deletes the discussion (once) and notifies the owner
     * (once) with {@link EventTypeName#CONTENT_REMOVED}.
     *
     * @param decision persisted logbook row
     * @param report report being closed
     * @param admin acting admin used for soft-delete attribution
     * @param removedDiscussionIds discussions already handled in this request
     */
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

    /**
     * Soft-deletes the reported discussion once per request and notifies the owner.
     * <p>
     * If this discussion was already processed in {@code removedDiscussionIds}, or
     * {@code deletedAt} is already set, the method returns without a second delete
     * or a second owner notification. Reports are still closed by the caller.
     *
     * @param admin acting admin stored as {@code deletedBy}
     * @param report report whose discussion is the remove target
     * @param decision logbook row used as the owner-notification event source
     * @param removedDiscussionIds discussions already removed in this request
     */
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

    private ModerationPayload createModerationPayload(List<ModerationAction> decisions){
        return new ModerationPayload(decisions.stream().map(this::createModerationDTO).collect(Collectors.toList()));
    }

    private ModerationDTO createModerationDTO(ModerationAction decision){
        return new ModerationDTO(
                decision.getActionId(),
                reportService.toReportDTO(List.of(decision.getReport())),
                userAccountManager.getUserAccountDTO(decision.getAdminUser()),
                decision.getModerateActionType(),
                decision.getAdminNotes(),
                decision.getCreatedAt()
        );
    }
}
