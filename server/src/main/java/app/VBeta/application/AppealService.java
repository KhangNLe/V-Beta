package app.VBeta.application;

import app.VBeta.api.dto.moderation.AppealDTO;
import app.VBeta.api.dto.moderation.AppealPayload;
import app.VBeta.api.dto.moderation.AppealRequest;
import app.VBeta.api.dto.moderation.ModerateAppealRequest;
import app.VBeta.api.dto.moderation.OwnerDeletionNoticeDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.moderation.AppealManager;
import app.VBeta.application.support.moderation.ModerationManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.appeal.Appeal;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * {@code AppealService} is the orchestration layer for content-owner appeals
 * ({@code POST /api/moderate/appeal}), admin appeal-queue reads
 * ({@code GET /api/moderate/appeal}), and admin appeal resolve
 * ({@code PATCH /api/moderate/appeal}).
 * <p>
 * Create is authenticated only: the caller must own the removed discussion.
 * Queue and detail authorize {@link ActionDefinition#VIEW_APPEALS}.
 * Resolve authorizes {@link ActionDefinition#MODERATE_APPEAL}.
 */
@Service
@Transactional
public class AppealService {
    private final UserAccountManager userAccountManager;
    private final AppealManager appealManager;
    private final AuthorizationService authorizationService;
    private final ReportManager reportManager;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final ModerationManager moderationManager;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    /**
     * Constructs a new {@code AppealService} with required collaborators.
     *
     * @param userAccountManager manager for appellant and admin account lookups
     * @param appealManager manager for appeal persistence
     * @param authorizationService service for {@code VIEW_APPEALS} checks
     * @param reportManager manager for report lookup and status updates
     * @param reportService service used to map the appealed report into a {@code ReportDTO}
     * @param notificationService service for appeal inbox writes
     * @param moderationManager manager for append-only logbook rows
     * @param climbingProblemDiscussionManager manager for discussion restore on approve
     */
    public AppealService(UserAccountManager userAccountManager,
                         AppealManager appealManager,
                         AuthorizationService authorizationService,
                         ReportManager reportManager,
                         ReportService reportService,
                         NotificationService notificationService,
                         ModerationManager moderationManager,
                         ClimbingProblemDiscussionManager climbingProblemDiscussionManager) {
        this.userAccountManager = userAccountManager;
        this.appealManager = appealManager;
        this.authorizationService = authorizationService;
        this.reportManager = reportManager;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.moderationManager = moderationManager;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
    }

    /**
     * Creates a one-time appeal for a {@code CONTENT_REMOVED} discussion report.
     * <p>
     * The caller must own the reported discussion. On success the report status
     * becomes {@link ReportStatus#APPEAL_PENDING} and admins are notified.
     *
     * @param appealRequest report id and owner reason
     * @param firebaseUid Firebase UID of the content owner
     * @throws RuntimeException when the account is missing, the report is missing
     *         or ineligible, or an appeal already exists
     */
    public void createAppeal(AppealRequest appealRequest, String firebaseUid) {
        UserAccount appealUser = userAccountManager.findUserAccount(firebaseUid);
        if (appealUser == null) {
            throw new RuntimeException("User not found");
        }

        Report report = reportManager.findById(appealRequest.reportId());
        if (!appealManager.isFirstAppeal(report)) {
            throw new RuntimeException("Appeal already exists");
        }

        validateAppealEligibility(report, appealUser);
        appealManager.createAppeal(appealRequest, report, appealUser);
        report.setReportStatus(ReportStatus.APPEAL_PENDING);
        reportManager.save(report);
        notificationService.saveAppealSubmittedNotification(report, appealUser);
    }

    /**
     * Returns the owner deletion notice for one report.
     * <p>
     * Authenticated only. The caller must own the reported discussion and the
     * report must be a removal/appeal outcome ({@code CONTENT_REMOVED},
     * {@code APPEAL_PENDING}, {@code CONTENT_RESTORED}, or {@code APPEAL_DENIED}).
     *
     * @param reportId report named in {@code /appeals?reportId=}
     * @param firebaseUid Firebase UID of the content owner
     * @return admin removal notes, content snapshot, and whether an appeal is still allowed
     * @throws RuntimeException when the account is missing or the report is ineligible
     */
    public OwnerDeletionNoticeDTO getOwnerDeletionNotice(Long reportId, String firebaseUid) {
        UserAccount caller = userAccountManager.findUserAccount(firebaseUid);
        if (caller == null) {
            throw new RuntimeException("User not found");
        }
        Report report = reportManager.findById(reportId);
        if (!isOwnerDeletionNoticeVisible(report, caller)) {
            throw new RuntimeException("Appeal is not allowed");
        }
        Appeal appeal = appealManager.findByReport(report).orElse(null);
        boolean canAppeal = report.getReportStatus() == ReportStatus.CONTENT_REMOVED && appeal == null;
        return new OwnerDeletionNoticeDTO(
                report.getReportId(),
                report.getReportStatus(),
                removalAdminReason(report),
                withDiscussionFallback(reportService.toReportDTO(List.of(report)), report),
                appeal == null ? null : appeal.getAppealStatus(),
                canAppeal
        );
    }

    /**
     * Returns one appeal by identifier.
     * <p>
     * Requires {@link ActionDefinition#VIEW_APPEALS}. The payload {@code appeals}
     * list has a single {@link AppealDTO}. Appeals filed by the viewing admin
     * are treated as missing.
     *
     * @param appealId appeal identifier
     * @param firebaseUid Firebase UID of the requesting admin
     * @return payload with that one appeal
     * @throws RuntimeException when the account is missing, unauthorized, or
     *         {@code appealId} does not exist / is hidden
     */
    public AppealPayload getUserAppeal(Long appealId, String firebaseUid) {
        UserAccount admin = requireAppealViewer(firebaseUid);
        Appeal appeal = appealManager.findById(appealId)
                .orElseThrow(() -> new RuntimeException("Appeal not found"));
        if (isHiddenFromViewer(appeal, admin)) {
            throw new RuntimeException("Appeal not found");
        }
        return toPayload(List.of(appeal));
    }

    /**
     * Returns OPEN appeals newest-first for the admin queue.
     * <p>
     * Requires {@link ActionDefinition#VIEW_APPEALS}. Appeals filed by the
     * viewing admin are omitted.
     *
     * @param firebaseUid Firebase UID of the requesting admin
     * @return payload of OPEN appeals (empty list when none are visible)
     * @throws RuntimeException when the account is missing or unauthorized
     */
    public AppealPayload getAppeals(String firebaseUid) {
        UserAccount admin = requireAppealViewer(firebaseUid);
        List<Appeal> appeals = appealManager.findOpenAppeals().stream()
                .filter(appeal -> !isHiddenFromViewer(appeal, admin))
                .toList();
        return toPayload(appeals);
    }

    /**
     * Applies an {@code APPROVED} or {@code DENIED} decision to one OPEN appeal.
     * <p>
     * Requires {@link ActionDefinition#MODERATE_APPEAL}. Approve restores the
     * discussion, sets the report to {@link ReportStatus#CONTENT_RESTORED}, and
     * notifies the owner with {@link EventTypeName#CONTENT_RESTORED}. Deny sets
     * the report to {@link ReportStatus#APPEAL_DENIED} and notifies the owner
     * with {@link EventTypeName#APPEAL_DENIED}. Both paths write a logbook row.
     *
     * @param moderateAppealRequest appeal id, decision, and required admin notes
     * @param firebaseUid Firebase UID of the acting admin
     * @throws RuntimeException when the account is missing, unauthorized, the
     *         appeal is missing/hidden, or the appeal is not {@code OPEN}
     */
    public void moderateAppeal(ModerateAppealRequest moderateAppealRequest, String firebaseUid) {
        UserAccount admin = userAccountManager.findUserAccount(firebaseUid);
        if (admin == null) {
            throw new RuntimeException("User not found");
        }
        authorizationService.authorize(admin, ActionDefinition.MODERATE_APPEAL);

        Appeal appeal = appealManager.findById(moderateAppealRequest.appealId())
                .orElseThrow(() -> new RuntimeException("Appeal not found"));
        if (isHiddenFromViewer(appeal, admin) || appeal.getAppealStatus() != AppealStatus.OPEN) {
            throw new RuntimeException("Appeal not found");
        }

        appeal.setAppealStatus(moderateAppealRequest.appealStatus());
        appeal.setAdminNote(moderateAppealRequest.adminReason());
        appeal.setReviewedBy(admin);
        appeal.setResolvedAt(Instant.now());
        appealManager.save(appeal);

        applyAppealDecision(appeal, admin, moderateAppealRequest);
    }

    /**
     * Updates the report, optional restore, logbook, and owner notification for
     * one resolved appeal.
     *
     * @param appeal persisted appeal whose status is already {@code APPROVED} or {@code DENIED}
     * @param admin acting admin used as logbook actor and event actor
     * @param request appeal-resolve payload supplying notes
     */
    private void applyAppealDecision(Appeal appeal, UserAccount admin, ModerateAppealRequest request) {
        Report report = appeal.getReport();
        if (appeal.getAppealStatus() == AppealStatus.APPROVED) {
            if (report.getDiscussion() != null) {
                climbingProblemDiscussionManager.restoreDiscussionRoot(report.getDiscussion().getDiscussionId());
            }
            reportManager.updateReportStatus(report, ReportStatus.CONTENT_RESTORED);
            ModerationAction decision = moderationManager.createAppealDecision(
                    report, admin, ModerateActionType.APPEAL_APPROVED, request.adminReason());
            notificationService.sendReportModerationNotification(decision, appeal.getAppealUser(), report,
                    EventTypeName.CONTENT_RESTORED);
        } else {
            reportManager.updateReportStatus(report, ReportStatus.APPEAL_DENIED);
            ModerationAction decision = moderationManager.createAppealDecision(
                    report, admin, ModerateActionType.APPEAL_DENIED, request.adminReason());
            notificationService.sendReportModerationNotification(decision, appeal.getAppealUser(), report,
                    EventTypeName.APPEAL_DENIED);
        }
    }

    /**
     * Resolves the caller and authorizes {@link ActionDefinition#VIEW_APPEALS}.
     *
     * @param firebaseUid Firebase UID of the requesting admin
     * @return matching account
     * @throws RuntimeException when the account is missing or unauthorized
     */
    private UserAccount requireAppealViewer(String firebaseUid) {
        UserAccount admin = userAccountManager.findUserAccount(firebaseUid);
        if (admin == null) {
            throw new RuntimeException("User not found");
        }
        authorizationService.authorize(admin, ActionDefinition.VIEW_APPEALS);
        return admin;
    }

    /**
     * Rejects appeals that are not a first-time restore request on a removed
     * discussion owned by {@code appealUser}.
     *
     * @param report report named in the request
     * @param appealUser authenticated caller
     * @throws RuntimeException when the report is not an eligible removal
     */
    private void validateAppealEligibility(Report report, UserAccount appealUser) {
        if (report.getTargetType() != ReportTargetType.DISCUSSION
                || report.getReportStatus() != ReportStatus.CONTENT_REMOVED
                || report.getDiscussion() == null
                || report.getDiscussion().getUserAccount() == null
                || !report.getDiscussion().getUserAccount().getId().equals(appealUser.getId())) {
            throw new RuntimeException("Appeal is not allowed");
        }
    }

    /**
     * Owner notice is visible for removal and later appeal outcomes on a
     * discussion the caller owns.
     */
    private boolean isOwnerDeletionNoticeVisible(Report report, UserAccount caller) {
        if (report.getTargetType() != ReportTargetType.DISCUSSION
                || report.getDiscussion() == null
                || report.getDiscussion().getUserAccount() == null
                || !report.getDiscussion().getUserAccount().getId().equals(caller.getId())) {
            return false;
        }
        ReportStatus status = report.getReportStatus();
        return status == ReportStatus.CONTENT_REMOVED
                || status == ReportStatus.APPEAL_PENDING
                || status == ReportStatus.CONTENT_RESTORED
                || status == ReportStatus.APPEAL_DENIED;
    }

    /**
     * Prefers {@code CONTENT_REMOVED} logbook notes, then the discussion
     * {@code deletedReason}.
     */
    private String removalAdminReason(Report report) {
        return moderationManager.findByReport(report).stream()
                .filter(action -> action.getModerateActionType() == ModerateActionType.CONTENT_REMOVED)
                .map(ModerationAction::getAdminNotes)
                .filter(notes -> notes != null && !notes.isBlank())
                .findFirst()
                .orElseGet(() -> {
                    String deletedReason = report.getDiscussion().getDeletedReason();
                    return deletedReason == null ? "" : deletedReason;
                });
    }

    /**
     * Soft-deleted discussions can omit the comment/beta child in
     * {@link ReportService#toReportDTO}; still expose type/id for the owner page.
     */
    private ReportDTO withDiscussionFallback(ReportDTO snapshot, Report report) {
        if (snapshot.discussion() != null || report.getDiscussion() == null) {
            return snapshot;
        }
        DiscussionRoot root = report.getDiscussion();
        UserDiscussionData fallback = new UserDiscussionData(
                root.getDiscussionId(),
                root.getUserAccount() == null ? null : root.getUserAccount().getId(),
                root.getUserAccount() == null ? null : root.getUserAccount().getUsername(),
                null,
                root.getDiscussionType(),
                "",
                root.getCreatedAt()
        );
        return new ReportDTO(
                snapshot.targetType(),
                fallback,
                snapshot.climbingProblem(),
                snapshot.wallSection(),
                snapshot.user(),
                snapshot.reporters()
        );
    }

    /**
     * Returns whether {@code viewer} filed this appeal and must not review it.
     *
     * @param appeal appeal being viewed
     * @param viewer viewing account
     * @return {@code true} when the viewer is the appellant
     */
    private boolean isHiddenFromViewer(Appeal appeal, UserAccount viewer) {
        return Objects.equals(appeal.getAppealUser().getId(), viewer.getId());
    }

    /**
     * Maps appeal entities into the API payload.
     *
     * @param appeals persisted appeals to serialize
     * @return payload whose {@code appeals} list matches {@code appeals}
     */
    private AppealPayload toPayload(List<Appeal> appeals) {
        return new AppealPayload(appeals.stream().map(this::convertToAppealDTO).toList());
    }

    /**
     * Maps one appeal to a DTO: id, the appealed report snapshot, owner, and reason.
     *
     * @param appeal persisted appeal
     * @return API appeal item
     */
    private AppealDTO convertToAppealDTO(Appeal appeal) {
        return new AppealDTO(
                appeal.getId(),
                reportService.toReportDTO(List.of(appeal.getReport())),
                userAccountManager.getUserAccountDTO(appeal.getAppealUser()),
                appeal.getReason()
        );
    }
}
