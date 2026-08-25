package app.VBeta.application;

import app.VBeta.api.dto.moderation.AppealDTO;
import app.VBeta.api.dto.moderation.AppealPayload;
import app.VBeta.api.dto.moderation.AppealRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.moderation.AppealManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.appeal.Appeal;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * {@code AppealService} is the orchestration layer for content-owner appeals
 * ({@code POST /api/moderate/appeal}) and admin appeal-queue reads
 * ({@code GET /api/moderate/appeal}).
 * <p>
 * Create is authenticated only: the caller must own the removed discussion.
 * Queue and detail authorize {@link ActionDefinition#VIEW_APPEALS}.
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

    /**
     * Constructs a new {@code AppealService} with required collaborators.
     *
     * @param userAccountManager manager for appellant and admin account lookups
     * @param appealManager manager for appeal persistence
     * @param authorizationService service for {@code VIEW_APPEALS} checks
     * @param reportManager manager for report lookup and status updates
     * @param reportService service used to map the appealed report into a {@code ReportDTO}
     * @param notificationService service for {@code APPEAL_SUBMITTED} admin inbox writes
     */
    public AppealService(UserAccountManager userAccountManager,
                         AppealManager appealManager,
                         AuthorizationService authorizationService,
                         ReportManager reportManager,
                         ReportService reportService,
                         NotificationService notificationService) {
        this.userAccountManager = userAccountManager;
        this.appealManager = appealManager;
        this.authorizationService = authorizationService;
        this.reportManager = reportManager;
        this.reportService = reportService;
        this.notificationService = notificationService;
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
