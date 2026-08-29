package app.VBeta.Integration_Test;

import app.VBeta.api.dto.moderation.ModerationDTO;
import app.VBeta.api.dto.moderation.ModerationPayload;
import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.ModerationService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.config.*;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
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
public class ModerationServiceTest {
    private static final String CLIMBER_UID = "testFirebaseUid";
    private static final String SETTER_UID = "testFirebaseUid2";
    private static final String ADMIN_UID = "testFirebaseUid3";
    private static final String ADMIN_NOTES = "Content does not make any sense.";

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private DiscussionRootManager discussionRootManager;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private ReportManager reportManager;

    @Autowired
    private ModerationRepository moderationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private GymRoleRepository gymRoleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(account);
        return account;
    }

    private ClimbingProblem getClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        return problem;
    }

    private DiscussionRoot createDiscussion(String firebaseUid, Long problemId, DiscussionType discussionType){
        UserAccount account = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(problemId);
        DiscussionRoot discussionRoot = discussionRootManager.createNewDiscussion(account, problem, discussionType);
        assertNotNull(discussionRoot);
        return discussionRoot;
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

    private Report createReport(DiscussionRoot discussion, UserAccount reporter){
        ReportRequest request = new ReportRequest(
               ReportTargetType.DISCUSSION,
                "Spammy",
                ReportCategoryName.SPAM,
                discussion.getDiscussionId()
        );

        Report report = reportManager.createReport(reporter, request);
        assertNotNull(report);
        assertEquals(ReportStatus.OPEN, report.getReportStatus());
        return report;
    }

    private Report reload(Report report) {
        return reportManager.findById(report.getReportId());
    }

    private DiscussionRoot reloadDiscussion(DiscussionRoot discussion) {
        return discussionRootRepository.findById(discussion.getDiscussionId()).orElseThrow();
    }

    private List<ModerationAction> actionsFor(Report report) {
        return moderationRepository.findAll().stream()
                .filter(action -> action.getReport().getReportId().equals(report.getReportId()))
                .toList();
    }

    private List<Notification> unreadFor(UserAccount user) {
        return notificationRepository.findAllUnreadByRecipientUser(user);
    }

    private long countEvents(UserAccount user, EventTypeName type) {
        return unreadFor(user).stream()
                .filter(notification -> notification.getEvent().getEventType().getEventTypeName() == type)
                .count();
    }

    private void moderate(List<Long> reportIds, ModerateActionType decision) {
        moderationService.createModerationForReportQueue(
                new ModerationRequest(reportIds, decision, ADMIN_NOTES),
                ADMIN_UID
        );
    }

    @Test
    @DisplayName("Admin dismiss closes each reporter row, writes logbook, and notifies only reporters")
    void dismissesEachOpenReportWithoutDeletingDiscussion() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report reportA = createReport(discussion, getUserAccount(CLIMBER_UID));
        Report reportB = createReport(discussion, getUserAccount(SETTER_UID));

        moderate(List.of(reportA.getReportId(), reportB.getReportId()), ModerateActionType.REPORT_DISMISSED);

        Report closedReportA = reload(reportA);
        Report closedReportB = reload(reportB);
        assertEquals(ReportStatus.DISMISSED, closedReportA.getReportStatus());
        assertEquals(ReportStatus.DISMISSED, closedReportB.getReportStatus());
        assertNotNull(closedReportA.getResolvedAt());
        assertNotNull(closedReportB.getResolvedAt());

        List<ModerationAction> actionsA = actionsFor(closedReportA);
        assertEquals(1, actionsA.size());
        assertEquals(ModerateActionType.REPORT_DISMISSED, actionsA.get(0).getModerateActionType());
        assertEquals(ADMIN_NOTES, actionsA.get(0).getAdminNotes());
        assertEquals(getUserAccount(ADMIN_UID).getId(), actionsA.get(0).getAdminUser().getId());
        assertEquals(1, actionsFor(closedReportB).size());

        assertEquals(1, countEvents(getUserAccount(CLIMBER_UID), EventTypeName.REPORT_DISMISSED));
        assertEquals(1, countEvents(getUserAccount(SETTER_UID), EventTypeName.REPORT_DISMISSED));
        assertEquals(0, countEvents(owner, EventTypeName.CONTENT_REMOVED));
        assertNull(reloadDiscussion(discussion).getDeletedAt());
    }

    @Test
    @DisplayName("Admin remove closes each reporter row, soft-deletes discussion once, and notifies owner once")
    void removesContentOnceWhenMultipleReportsShareDiscussion() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report reportA = createReport(discussion, getUserAccount(CLIMBER_UID));
        Report reportB = createReport(discussion, getUserAccount(SETTER_UID));

        moderate(List.of(reportA.getReportId(), reportB.getReportId()), ModerateActionType.CONTENT_REMOVED);

        assertEquals(ReportStatus.CONTENT_REMOVED, reload(reportA).getReportStatus());
        assertEquals(ReportStatus.CONTENT_REMOVED, reload(reportB).getReportStatus());

        DiscussionRoot deleted = reloadDiscussion(discussion);
        assertNotNull(deleted.getDeletedAt());
        assertEquals(getUserAccount(ADMIN_UID).getId(), deleted.getDeletedBy().getId());
        assertEquals("Admin approved the content deletion due to report of SPAM.", deleted.getDeletedReason());

        assertEquals(1, countEvents(getUserAccount(CLIMBER_UID), EventTypeName.REPORT_APPROVED));
        assertEquals(1, countEvents(getUserAccount(SETTER_UID), EventTypeName.REPORT_APPROVED));
        assertEquals(1, countEvents(owner, EventTypeName.CONTENT_REMOVED));
        assertEquals(0, countEvents(owner, EventTypeName.REPORT_APPROVED));
    }

    @Test
    @DisplayName("Already-deleted discussion still closes reports and does not notify owner again")
    void closesReportsWhenDiscussionAlreadyDeleted() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        UserAccount admin = getUserAccount(ADMIN_UID);
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = createReport(discussion, getUserAccount(CLIMBER_UID));
        climbingProblemDiscussionManager.softDeleteDiscussionRoot(admin, discussion.getDiscussionId(), "pre-deleted");

        moderate(List.of(report.getReportId()), ModerateActionType.CONTENT_REMOVED);

        assertEquals(ReportStatus.CONTENT_REMOVED, reload(report).getReportStatus());
        assertEquals("pre-deleted", reloadDiscussion(discussion).getDeletedReason());
        assertEquals(1, countEvents(getUserAccount(CLIMBER_UID), EventTypeName.REPORT_APPROVED));
        assertEquals(0, countEvents(owner, EventTypeName.CONTENT_REMOVED));
    }

    @Test
    @DisplayName("Unknown, hidden, and already-closed report IDs are skipped")
    void skipsIneligibleReportIdsWithoutFailingTheBatch() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report openReport = createReport(discussion, getUserAccount(CLIMBER_UID));
        Report alreadyClosed = createReport(
                discussionRootManager.createNewDiscussion(owner, getClimbingProblem(1L), DiscussionType.COMMENT),
                getUserAccount(SETTER_UID)
        );
        alreadyClosed.setReportStatus(ReportStatus.DISMISSED);
        reportManager.save(alreadyClosed);

        DiscussionRoot adminOwned = createDiscussion(ADMIN_UID, 1L, DiscussionType.COMMENT);
        Report hiddenFromAdmin = createReport(adminOwned, getUserAccount(CLIMBER_UID));

        assertDoesNotThrow(() -> moderate(
                List.of(999_999L, hiddenFromAdmin.getReportId(), alreadyClosed.getReportId(), openReport.getReportId()),
                ModerateActionType.REPORT_DISMISSED
        ));

        assertEquals(ReportStatus.DISMISSED, reload(openReport).getReportStatus());
        assertEquals(ReportStatus.DISMISSED, reload(alreadyClosed).getReportStatus());
        assertEquals(ReportStatus.OPEN, reload(hiddenFromAdmin).getReportStatus());
        assertEquals(1, actionsFor(reload(openReport)).size());
        assertEquals(0, actionsFor(reload(hiddenFromAdmin)).size());
    }

    @Test
    @DisplayName("Admin cannot close a report they filed")
    void skipsReportFiledByActingAdmin() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report adminFiled = createReport(discussion, getUserAccount(ADMIN_UID));

        moderate(List.of(adminFiled.getReportId()), ModerateActionType.REPORT_DISMISSED);

        assertEquals(ReportStatus.OPEN, reload(adminFiled).getReportStatus());
        assertTrue(actionsFor(adminFiled).isEmpty());
        assertEquals(0, countEvents(getUserAccount(ADMIN_UID), EventTypeName.REPORT_DISMISSED));
    }

    @Test
    @DisplayName("Appeal decisions are rejected before any report is closed")
    void rejectsAppealDecisions() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = createReport(discussion, getUserAccount(CLIMBER_UID));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> moderate(
                List.of(report.getReportId()),
                ModerateActionType.APPEAL_APPROVED
        ));
        assertEquals("Appeal decisions are not supported on this endpoint.", ex.getMessage());
        assertEquals(ReportStatus.OPEN, reload(report).getReportStatus());
        assertTrue(actionsFor(report).isEmpty());
    }

    @Test
    @DisplayName("Climber cannot moderate reports")
    void rejectsClimberModerator() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = createReport(discussion, getUserAccount(SETTER_UID));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                moderationService.createModerationForReportQueue(
                        new ModerationRequest(List.of(report.getReportId()),
                                ModerateActionType.REPORT_DISMISSED, ADMIN_NOTES),
                        CLIMBER_UID
                ));
        assertTrue(ex.getMessage().contains("not allowed to perform action"));
        assertEquals(ReportStatus.OPEN, reload(report).getReportStatus());
    }

    @Test
    @DisplayName("Unknown firebase uid fails before the report loop")
    void rejectsUnknownAdminUid() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                moderationService.createModerationForReportQueue(
                        new ModerationRequest(List.of(1L), ModerateActionType.REPORT_DISMISSED, ADMIN_NOTES),
                        "missing-uid"
                ));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("Admin logbook is empty when no decisions have been recorded")
    void returnsEmptyLogbookWhenNoDecisionsExist() {
        ModerationPayload payload = moderationService.getLogbook(ADMIN_UID, 1);

        assertNotNull(payload);
        assertNotNull(payload.moderationLogs());
        assertTrue(payload.moderationLogs().isEmpty());
    }

    @Test
    @DisplayName("Admin logbook returns decisions newest first with report snapshot and notes")
    void returnsLogbookEntriesNewestFirst() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report reportA = createReport(discussion, getUserAccount(CLIMBER_UID));
        Report reportB = createReport(
                discussionRootManager.createNewDiscussion(owner, getClimbingProblem(1L), DiscussionType.COMMENT),
                getUserAccount(SETTER_UID)
        );

        moderate(List.of(reportA.getReportId()), ModerateActionType.REPORT_DISMISSED);
        moderate(List.of(reportB.getReportId()), ModerateActionType.CONTENT_REMOVED);

        ModerationAction first = actionsFor(reload(reportA)).get(0);
        ModerationAction second = actionsFor(reload(reportB)).get(0);

        ModerationPayload payload = moderationService.getLogbook(ADMIN_UID, 1);
        assertEquals(2, payload.moderationLogs().size());

        ModerationDTO newest = payload.moderationLogs().get(0);
        ModerationDTO older = payload.moderationLogs().get(1);
        assertEquals(second.getActionId(), newest.moderationId());
        assertEquals(first.getActionId(), older.moderationId());
        assertFalse(newest.createdAt().isBefore(older.createdAt()));

        assertEquals(ModerateActionType.CONTENT_REMOVED, newest.decision());
        assertEquals(ADMIN_NOTES, newest.adminNote());
        assertEquals(getUserAccount(ADMIN_UID).getId(), newest.resolvedBy().userId());
        assertEquals(ReportTargetType.DISCUSSION, newest.report().targetType());
        assertEquals(1, newest.report().reporters().size());
        assertEquals(reportB.getReportId(), newest.report().reporters().get(0).reportId());

        assertEquals(ModerateActionType.REPORT_DISMISSED, older.decision());
        assertEquals(reportA.getReportId(), older.report().reporters().get(0).reportId());
    }

    @Test
    @DisplayName("Admin logbook page two is empty when fewer than 25 decisions exist")
    void returnsEmptyLogbookOnLaterPage() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = createReport(discussion, getUserAccount(CLIMBER_UID));
        moderate(List.of(report.getReportId()), ModerateActionType.REPORT_DISMISSED);

        ModerationPayload pageOne = moderationService.getLogbook(ADMIN_UID, 1);
        ModerationPayload pageTwo = moderationService.getLogbook(ADMIN_UID, 2);

        assertEquals(1, pageOne.moderationLogs().size());
        assertTrue(pageTwo.moderationLogs().isEmpty());
    }

    @Test
    @DisplayName("Admin can load one logbook row by moderation id")
    void returnsSingleLogByModerationId() {
        UserAccount owner = createClimber("owner-" + UUID.randomUUID());
        DiscussionRoot discussion = discussionRootManager.createNewDiscussion(
                owner, getClimbingProblem(1L), DiscussionType.COMMENT);
        Report report = createReport(discussion, getUserAccount(CLIMBER_UID));
        moderate(List.of(report.getReportId()), ModerateActionType.REPORT_DISMISSED);

        Long moderationId = actionsFor(reload(report)).get(0).getActionId();
        ModerationPayload payload = moderationService.getModerationLog(ADMIN_UID, moderationId);

        assertEquals(1, payload.moderationLogs().size());
        ModerationDTO log = payload.moderationLogs().get(0);
        assertEquals(moderationId, log.moderationId());
        assertEquals(ModerateActionType.REPORT_DISMISSED, log.decision());
        assertEquals(ADMIN_NOTES, log.adminNote());
        assertEquals(getUserAccount(ADMIN_UID).getId(), log.resolvedBy().userId());
        assertEquals(ReportTargetType.DISCUSSION, log.report().targetType());
        assertEquals(report.getReportId(), log.report().reporters().get(0).reportId());
        assertEquals(ReportCategoryName.SPAM, log.report().reporters().get(0).categoryName());
        assertEquals("Spammy", log.report().reporters().get(0).reportReason());
    }

    @Test
    @DisplayName("Unknown moderation id fails after authorization")
    void rejectsUnknownModerationId() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                moderationService.getModerationLog(ADMIN_UID, 999_999L));
        assertEquals("Moderation not found", ex.getMessage());
    }

    @Test
    @DisplayName("Climber cannot view the moderation logbook")
    void rejectsClimberLogbookViewer() {
        RuntimeException listEx = assertThrows(RuntimeException.class, () ->
                moderationService.getLogbook(CLIMBER_UID, 1));
        assertTrue(listEx.getMessage().contains("not allowed to perform action"));

        RuntimeException detailEx = assertThrows(RuntimeException.class, () ->
                moderationService.getModerationLog(CLIMBER_UID, 1L));
        assertTrue(detailEx.getMessage().contains("not allowed to perform action"));
    }

    @Test
    @DisplayName("Unknown firebase uid cannot view the moderation logbook")
    void rejectsUnknownUidForLogbook() {
        RuntimeException listEx = assertThrows(RuntimeException.class, () ->
                moderationService.getLogbook("missing-uid", 1));
        assertEquals("User not found", listEx.getMessage());

        RuntimeException detailEx = assertThrows(RuntimeException.class, () ->
                moderationService.getModerationLog("missing-uid", 1L));
        assertEquals("User not found", detailEx.getMessage());
    }
}
