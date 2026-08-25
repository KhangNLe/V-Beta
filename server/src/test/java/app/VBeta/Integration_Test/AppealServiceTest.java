package app.VBeta.Integration_Test;

import app.VBeta.api.dto.moderation.AppealDTO;
import app.VBeta.api.dto.moderation.AppealPayload;
import app.VBeta.api.dto.moderation.AppealRequest;
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
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.NotificationRepository;
import app.VBeta.repository.GymRoleRepository;
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
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
        "spring.datasource.username=${SQL_USERNAME:postgres}",
        "spring.datasource.password=${SQL_PASSWORD:postgres}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
public class AppealServiceTest {
    private static final String CLIMBER_UID = "testFirebaseUid";
    private static final String ADMIN_UID = "testFirebaseUid3";
    private static final String APPEAL_REASON = "This was a joke, please restore.";

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
        GymRole role = gymRoleRepository.findByRoleType(RoleType.CLIMBER).orElseThrow();
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
}
