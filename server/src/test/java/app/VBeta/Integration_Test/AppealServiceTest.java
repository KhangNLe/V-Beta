package app.VBeta.Integration_Test;

import app.VBeta.api.dto.moderation.AppealDTO;
import app.VBeta.api.dto.moderation.AppealPayload;
import app.VBeta.api.dto.moderation.AppealRequest;
import app.VBeta.api.dto.moderation.ModerateAppealRequest;
import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.AppealService;
import app.VBeta.application.ModerationService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.config.TestGcpStorageConfig;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.appeal.Appeal;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.AppealRepository;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.GymRoleRepository;
import app.VBeta.repository.ModerationRepository;
import app.VBeta.repository.NotificationRepository;
import app.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class AppealServiceTest {
    private static final String CLIMBER_UID = "testFirebaseUid";
    private static final String ADMIN_UID = "testFirebaseUid3";
    private static final String APPEAL_REASON = "This was a joke, please restore.";
    private static final String ADMIN_NOTES = "Reviewed the appeal.";

    @Autowired
    private AppealService appealService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private DiscussionRootManager discussionRootManager;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private ReportManager reportManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AppealRepository appealRepository;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private ModerationRepository moderationRepository;

    @Autowired
    private GymRoleRepository gymRoleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount getUserAccount(String firebaseUid) {
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(account);
        return account;
    }

    private ClimbingProblem getClimbingProblem(Long problemId) {
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        return problem;
    }

    private UserAccount createClimber(String firebaseUid) {
        return createUser(firebaseUid, RoleType.CLIMBER);
    }

    private UserAccount createAdmin(String firebaseUid) {
        return createUser(firebaseUid, RoleType.ADMIN);
    }

    private UserAccount createUser(String firebaseUid, RoleType roleType) {
        GymRole role = gymRoleRepository.findByRoleType(roleType).orElseThrow();
        return userAccountRepository.saveAndFlush(UserAccount.builder()
                .firebaseUid(firebaseUid)
                .email(firebaseUid + "@gmail.com")
                .username(("o" + firebaseUid.replace("-", "")).substring(0, 24))
                .gymRole(role)
                .build());
    }

    private Report createRemovedReport(UserAccount owner, UserAccount reporter) {
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = reportManager.createReport(reporter, new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy",
                ReportCategoryName.SPAM,
                discussion.getDiscussionId()
        ));
        moderationService.createModerationForReportQueue(
                new ModerationRequest(List.of(report.getReportId()), ModerateActionType.CONTENT_REMOVED,
                        "Content does not make any sense."),
                ADMIN_UID
        );
        return reportManager.findById(report.getReportId());
    }

    private Long createOpenAppeal(UserAccount owner, Report removed) {
        appealService.createAppeal(new AppealRequest(removed.getReportId(), APPEAL_REASON), owner.getFirebaseUid());
        return appealService.getAppeals(ADMIN_UID).appeals().stream()
                .filter(appeal -> owner.getId().equals(appeal.appealUser().userId()))
                .findFirst()
                .orElseThrow()
                .appealId();
    }

    private Appeal reloadAppeal(Long appealId) {
        return appealRepository.findById(appealId).orElseThrow();
    }

    private DiscussionRoot reloadDiscussion(Report report) {
        return discussionRootRepository.findById(report.getDiscussion().getDiscussionId()).orElseThrow();
    }

    private List<ModerationAction> appealActionsFor(Report report) {
        return moderationRepository.findAll().stream()
                .filter(action -> action.getReport().getReportId().equals(report.getReportId()))
                .filter(action -> action.getModerateActionType() == ModerateActionType.APPEAL_APPROVED
                        || action.getModerateActionType() == ModerateActionType.APPEAL_DENIED)
                .toList();
    }

    private long countEvents(UserAccount user, EventTypeName type) {
        return notificationRepository.findAllUnreadByRecipientUser(user).stream()
                .map(Notification::getEvent)
                .filter(event -> event.getEventType().getEventTypeName() == type)
                .count();
    }

    @Test
    @DisplayName("Owner can appeal a removed discussion once and admins are notified")
    void ownerCreatesFirstAppealAndNotifiesAdmins() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));

        appealService.createAppeal(new AppealRequest(removed.getReportId(), APPEAL_REASON), owner.getFirebaseUid());

        assertEquals(ReportStatus.APPEAL_PENDING, reportManager.findById(removed.getReportId()).getReportStatus());
        AppealPayload payload = appealService.getAppeals(ADMIN_UID);
        assertEquals(1, payload.appeals().size());
        AppealDTO appeal = payload.appeals().get(0);
        assertEquals(APPEAL_REASON, appeal.appealReason());
        assertEquals(owner.getId(), appeal.appealUser().userId());
        assertEquals(1, countEvents(getUserAccount(ADMIN_UID), EventTypeName.APPEAL_SUBMITTED));
    }

    @Test
    @DisplayName("A second appeal on the same report is rejected")
    void rejectsDuplicateAppeal() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        AppealRequest request = new AppealRequest(removed.getReportId(), APPEAL_REASON);
        appealService.createAppeal(request, owner.getFirebaseUid());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> appealService.createAppeal(request, owner.getFirebaseUid()));
        assertEquals("Appeal already exists", ex.getMessage());
    }

    @Test
    @DisplayName("Non-owners and non-removed reports cannot be appealed")
    void rejectsIneligibleAppeals() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        UserAccount other = createClimber("other-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));

        RuntimeException notOwner = assertThrows(RuntimeException.class, () ->
                appealService.createAppeal(new AppealRequest(removed.getReportId(), APPEAL_REASON),
                        other.getFirebaseUid()));
        assertEquals("Appeal is not allowed", notOwner.getMessage());

        DiscussionRoot openDiscussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report openReport = reportManager.createReport(getUserAccount(CLIMBER_UID), new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy",
                ReportCategoryName.SPAM,
                openDiscussion.getDiscussionId()
        ));
        RuntimeException stillOpen = assertThrows(RuntimeException.class, () ->
                appealService.createAppeal(new AppealRequest(openReport.getReportId(), APPEAL_REASON),
                        owner.getFirebaseUid()));
        assertEquals("Appeal is not allowed", stillOpen.getMessage());
    }

    @Test
    @DisplayName("Admin can read one appeal by id")
    void adminReadsAppealById() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        appealService.createAppeal(new AppealRequest(removed.getReportId(), APPEAL_REASON), owner.getFirebaseUid());
        Long appealId = appealService.getAppeals(ADMIN_UID).appeals().get(0).appealId();

        AppealPayload payload = appealService.getUserAppeal(appealId, ADMIN_UID);
        assertEquals(1, payload.appeals().size());
        assertEquals(appealId, payload.appeals().get(0).appealId());
    }

    @Test
    @DisplayName("Unknown appeal id and missing VIEW_APPEALS map to not found")
    void rejectsUnknownIdAndUnauthorizedViewer() {
        RuntimeException missing = assertThrows(RuntimeException.class,
                () -> appealService.getUserAppeal(999_999L, ADMIN_UID));
        assertEquals("Appeal not found", missing.getMessage());

        RuntimeException unauthorized = assertThrows(RuntimeException.class,
                () -> appealService.getAppeals(CLIMBER_UID));
        assertTrue(unauthorized.getMessage().contains("not allowed"));
    }

    @Test
    @DisplayName("Admin approve restores discussion, writes logbook, and notifies the owner")
    void approvesAppealAndRestoresDiscussion() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        assertNotNull(reloadDiscussion(removed).getDeletedAt());
        Long appealId = createOpenAppeal(owner, removed);

        appealService.moderateAppeal(
                new ModerateAppealRequest(appealId, AppealStatus.APPROVED, ADMIN_NOTES),
                ADMIN_UID
        );

        Appeal appeal = reloadAppeal(appealId);
        assertEquals(AppealStatus.APPROVED, appeal.getAppealStatus());
        assertEquals(ADMIN_NOTES, appeal.getAdminNote());
        assertEquals(getUserAccount(ADMIN_UID).getId(), appeal.getReviewedBy().getId());
        assertNotNull(appeal.getResolvedAt());
        assertEquals(ReportStatus.CONTENT_RESTORED, reportManager.findById(removed.getReportId()).getReportStatus());
        assertNull(reloadDiscussion(removed).getDeletedAt());

        List<ModerationAction> decisions = appealActionsFor(removed);
        assertEquals(1, decisions.size());
        assertEquals(ModerateActionType.APPEAL_APPROVED, decisions.get(0).getModerateActionType());
        assertEquals(ADMIN_NOTES, decisions.get(0).getAdminNotes());
        assertEquals(1, countEvents(owner, EventTypeName.CONTENT_RESTORED));
        assertEquals(0, appealService.getAppeals(ADMIN_UID).appeals().stream()
                .filter(item -> appealId.equals(item.appealId()))
                .count());
        assertEquals(appealId, appealService.getUserAppeal(appealId, ADMIN_UID).appeals().get(0).appealId());
    }

    @Test
    @DisplayName("Admin deny keeps content removed, writes logbook, and notifies the owner")
    void deniesAppealWithoutRestoringDiscussion() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        Long appealId = createOpenAppeal(owner, removed);

        appealService.moderateAppeal(
                new ModerateAppealRequest(appealId, AppealStatus.DENIED, ADMIN_NOTES),
                ADMIN_UID
        );

        Appeal appeal = reloadAppeal(appealId);
        assertEquals(AppealStatus.DENIED, appeal.getAppealStatus());
        assertEquals(ReportStatus.APPEAL_DENIED, reportManager.findById(removed.getReportId()).getReportStatus());
        assertNotNull(reloadDiscussion(removed).getDeletedAt());

        List<ModerationAction> decisions = appealActionsFor(removed);
        assertEquals(1, decisions.size());
        assertEquals(ModerateActionType.APPEAL_DENIED, decisions.get(0).getModerateActionType());
        assertEquals(1, countEvents(owner, EventTypeName.APPEAL_DENIED));
        assertEquals(0, countEvents(owner, EventTypeName.CONTENT_RESTORED));
    }

    @Test
    @DisplayName("A second decision on the same appeal is rejected")
    void rejectsSecondAppealDecision() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        Long appealId = createOpenAppeal(owner, removed);
        ModerateAppealRequest request = new ModerateAppealRequest(appealId, AppealStatus.DENIED, ADMIN_NOTES);
        appealService.moderateAppeal(request, ADMIN_UID);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> appealService.moderateAppeal(request, ADMIN_UID));
        assertEquals("Appeal not found", ex.getMessage());
    }

    @Test
    @DisplayName("Missing MODERATE_APPEAL and unknown appeal id map to not found")
    void rejectsUnauthorizedOrUnknownAppealDecision() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        Report removed = createRemovedReport(owner, getUserAccount(CLIMBER_UID));
        Long appealId = createOpenAppeal(owner, removed);

        RuntimeException unauthorized = assertThrows(RuntimeException.class, () ->
                appealService.moderateAppeal(
                        new ModerateAppealRequest(appealId, AppealStatus.APPROVED, ADMIN_NOTES),
                        CLIMBER_UID));
        assertTrue(unauthorized.getMessage().contains("not allowed"));
        assertEquals(AppealStatus.OPEN, reloadAppeal(appealId).getAppealStatus());

        RuntimeException missing = assertThrows(RuntimeException.class, () ->
                appealService.moderateAppeal(
                        new ModerateAppealRequest(999_999L, AppealStatus.DENIED, ADMIN_NOTES),
                        ADMIN_UID));
        assertEquals("Appeal not found", missing.getMessage());
    }

    @Test
    @DisplayName("An admin cannot view or decide an appeal they filed")
    void hidesAppealFiledByViewingAdmin() {
        UserAccount ownerAdmin = createAdmin("admin-" + UUID.randomUUID());
        Report removed = createRemovedReport(ownerAdmin, getUserAccount(CLIMBER_UID));
        appealService.createAppeal(
                new AppealRequest(removed.getReportId(), APPEAL_REASON), ownerAdmin.getFirebaseUid());
        Long appealId = appealService.getAppeals(ADMIN_UID).appeals().stream()
                .filter(appeal -> ownerAdmin.getId().equals(appeal.appealUser().userId()))
                .findFirst()
                .orElseThrow()
                .appealId();

        assertTrue(appealService.getAppeals(ownerAdmin.getFirebaseUid()).appeals().stream()
                .noneMatch(appeal -> appealId.equals(appeal.appealId())));

        RuntimeException hiddenRead = assertThrows(RuntimeException.class,
                () -> appealService.getUserAppeal(appealId, ownerAdmin.getFirebaseUid()));
        assertEquals("Appeal not found", hiddenRead.getMessage());

        RuntimeException hiddenDecision = assertThrows(RuntimeException.class, () ->
                appealService.moderateAppeal(
                        new ModerateAppealRequest(appealId, AppealStatus.APPROVED, ADMIN_NOTES),
                        ownerAdmin.getFirebaseUid()));
        assertEquals("Appeal not found", hiddenDecision.getMessage());
        assertEquals(AppealStatus.OPEN, reloadAppeal(appealId).getAppealStatus());
    }
}
