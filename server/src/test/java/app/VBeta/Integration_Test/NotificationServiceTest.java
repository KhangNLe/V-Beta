package app.VBeta.Integration_Test;

import app.VBeta.api.dto.notification.NotificationClickKind;
import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.ReportService;
import app.VBeta.application.NotificationService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.events.NotificationManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.notification.EventTargetType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.GymRoleRepository;
import app.VBeta.repository.NotificationRepository;
import app.VBeta.repository.ReportRepository;
import app.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import app.VBeta.config.TestGcpStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class NotificationServiceTest {
    @Autowired
    ReportService reportService;

    @Autowired
    DiscussionRootManager discussionRootManager;

    @Autowired
    ClimbingProblemManager climbingProblemManager;

    @Autowired
    UserAccountManager userAccountManager;

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    NotificationManager notificationManager;
    @Autowired
    private GymRoleRepository gymRoleRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(user);
        return user;
    }

    private ClimbingProblem getClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        return problem;
    }

    private DiscussionRoot createDiscussionRoot(String firebaseUid, Long problemId, DiscussionType type){
        UserAccount user = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(problemId);
        DiscussionRoot discussionRoot = discussionRootManager.createNewDiscussion(user, problem, type);
        assertNotNull(discussionRoot);
        return discussionRoot;
    }

    private UserAccount createNewAdmin(){
        UserAccount admin = new UserAccount();
        admin.setEmail("test@gmail.com");
        admin.setFirebaseUid("teste3tstet");
        Optional<GymRole> role = gymRoleRepository.findByRoleType(RoleType.ADMIN);
        assertTrue(role.isPresent());
        admin.setGymRole(role.get());
        admin.setUsername("admin2");
        return admin;
    }

    @Test
    @DisplayName("Test getting notification for admin from a user report")
    void testAdminNotificationFromUserReport(){
        UserAccount user1 = getUserAccount("testFirebaseUid");
        UserAccount user2 = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        assertEquals(RoleType.ADMIN, admin.getGymRole().getRoleType());

        List<Notification> adminNoti = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(0, adminNoti.size());

        DiscussionRoot discussionRoot = createDiscussionRoot(user1.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        ReportRequest report = new ReportRequest(
            ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        reportService.createNewReport(report, user2.getFirebaseUid());

        adminNoti = notificationManager.getUserUnreadNotifications(admin);
        assertNotNull(adminNoti);
        assertEquals(1, adminNoti.size());
        assertEquals(EventTypeName.REPORT_CREATED, adminNoti.get(0).getEvent().getEventType().getEventTypeName());
        assertNull(adminNoti.get(0).getReadAt());

        List<Notification> user1Noti =  notificationManager.getUserUnreadNotifications(user1);
        List<Notification> user2Noti = notificationManager.getUserUnreadNotifications(user2);

        assertEquals(0, user1Noti.size());
        assertEquals(0, user2Noti.size());
    }

    @Test
    @DisplayName("Admin report a discussion content and not getting the notification for the report")
    void testAdminReportADiscussionContent(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount admin2 = createNewAdmin();
        UserAccount user =  getUserAccount("testFirebaseUid2");
        userAccountRepository.saveAndFlush(admin2);

        DiscussionRoot discussionRoot = createDiscussionRoot(user.getFirebaseUid(), 1L, DiscussionType.BETA);

        ReportRequest report = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "tst",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        reportService.createNewReport(report, admin2.getFirebaseUid());
        List<Notification> admin1Inbox = notificationManager.getUserUnreadNotifications(admin);
        List<Notification> admin2Inbox = notificationManager.getUserUnreadNotifications(admin2);

        assertEquals(1, admin1Inbox.size());
        assertEquals(0, admin2Inbox.size());
    }

    @Test
    @DisplayName("Test getting notification from unknown user firebaseUid")
    void testUnknownUserFirebaseUid(){
        assertThrows(
                RuntimeException.class,
                () -> notificationService.getQuickNotifications("fakeUid")
        );
    }

    @Test
    @DisplayName("Admin quick notifications include REPORT_CREATED and omit report reason")
    void testAdminQuickNotificationsAfterUserReport(){
        UserAccount reporter = getUserAccount("testFirebaseUid2");
        UserAccount contentOwner = getUserAccount("testFirebaseUid");
        String secretReason = "Secret reason that must not leak";

        DiscussionRoot discussionRoot = createDiscussionRoot(
                contentOwner.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                secretReason,
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );
        reportService.createNewReport(request, reporter.getFirebaseUid());

        List<QuickNotificationDTO> inbox =
                notificationService.getQuickNotifications("testFirebaseUid3");

        assertEquals(1, inbox.size());
        assertEquals(EventTypeName.REPORT_CREATED.name(), inbox.get(0).summary().eventTypeName());
        assertNotNull(inbox.get(0).summary().description());
        assertFalse(inbox.get(0).summary().description().contains(secretReason));
        assertNotNull(inbox.get(0).createdAt());
    }

    @Test
    @DisplayName("REPORT_CREATED event targets the report and uses the reporter as actor")
    void testReportCreatedEventActorAndTarget(){
        UserAccount reporter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot(
                "testFirebaseUid", 1L, DiscussionType.COMMENT);

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );
        reportService.createNewReport(request, reporter.getFirebaseUid());

        List<Notification> adminInbox = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(1, adminInbox.size());

        var event = adminInbox.get(0).getEvent();
        assertEquals(EventTargetType.REPORT, event.getTargetType());
        assertNotNull(event.getReport());
        assertEquals(reporter.getId(), event.getActorUser().getId());
        assertNotEquals(admin.getId(), event.getActorUser().getId());
    }

    @Test
    @DisplayName("Two reports produce two unread admin notifications")
    void testTwoReportsProduceTwoAdminNotifications(){
        UserAccount reporter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot first = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.COMMENT);
        DiscussionRoot second = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.BETA);

        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "first",
                        ReportCategoryName.OFF_TOPIC,
                        first.getDiscussionId()),
                reporter.getFirebaseUid());
        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "second",
                        ReportCategoryName.SPAM,
                        second.getDiscussionId()),
                reporter.getFirebaseUid());

        List<Notification> adminInbox = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(2, adminInbox.size());
        assertTrue(adminInbox.stream().allMatch(notification ->
                notification.getEvent().getEventType().getEventTypeName() == EventTypeName.REPORT_CREATED));
    }

    @Test
    @DisplayName("Climber quick notifications are empty after another user reports")
    void testClimberQuickNotificationsRemainEmpty(){
        DiscussionRoot discussionRoot = createDiscussionRoot(
                "testFirebaseUid", 1L, DiscussionType.COMMENT);

        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.OFF_TOPIC,
                        discussionRoot.getDiscussionId()),
                "testFirebaseUid2");

        List<QuickNotificationDTO> climberInbox =
                notificationService.getQuickNotifications("testFirebaseUid");
        List<QuickNotificationDTO> setterInbox =
                notificationService.getQuickNotifications("testFirebaseUid2");

        assertTrue(climberInbox.isEmpty());
        assertTrue(setterInbox.isEmpty());
    }

    @Test
    @DisplayName("Read notifications are excluded from unread inbox")
    void testReadNotificationsAreExcludedFromUnreadInbox(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot(
                "testFirebaseUid", 1L, DiscussionType.COMMENT);

        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.OFF_TOPIC,
                        discussionRoot.getDiscussionId()),
                "testFirebaseUid2");

        List<Notification> unread = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(1, unread.size());

        Notification notification = unread.get(0);
        notification.setReadAt(Instant.now());
        notificationRepository.saveAndFlush(notification);

        List<Notification> afterRead = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(0, afterRead.size());
    }

    @Test
    @DisplayName("User read notification")
    void testUserReadNotification(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.BETA);
        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.SPAM,
                        discussionRoot.getDiscussionId()
                ),
                "testFirebaseUid2"
        );

        List<Notification> unread = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(1, unread.size());

        Notification unReadNoti = unread.get(0);
        assertNull(unReadNoti.getReadAt());
        assertEquals(unReadNoti.getRecipient(), admin);
        notificationService.updateNotificationToRead(admin.getFirebaseUid(), unReadNoti.getNotificationId());

        List<Notification> afterRead = notificationManager.getUserUnreadNotifications(admin);
        assertTrue(afterRead.isEmpty());

        Notification readNoti = notificationRepository.findById(unReadNoti.getNotificationId()).orElse(null);
        assertNotNull(readNoti);
        assertNotNull(readNoti.getReadAt());
        assertEquals(unReadNoti.getNotificationId(), readNoti.getNotificationId());
        assertEquals(unReadNoti.getEvent(), readNoti.getEvent());
        assertEquals(unReadNoti.getRecipient(), readNoti.getRecipient());
    }

    @Test
    @DisplayName("User attempt to mark notification that is not theirs as read")
    void testUserAttemptToMarkNotificationThatIsNotTheirsAsRead(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.BETA);
        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.SPAM,
                        discussionRoot.getDiscussionId()
                ),
                "testFirebaseUid2"
        );

        List<Notification> unread = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(1, unread.size());

        assertThrows(RuntimeException.class, () ->
                notificationService.updateNotificationToRead(
                        "testFirebaseUid2", unread.get(0).getNotificationId()
                )
        );

        Notification notification = notificationRepository.findById(unread.get(0).getNotificationId()).orElse(null);
        assertNotNull(notification);
        assertNull(notification.getReadAt());
    }

    @Test
    @DisplayName("User send update read notification attempt twice")
    void testUserSendUpdateReadNotification(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.BETA);
        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.SPAM,
                        discussionRoot.getDiscussionId()
                ),
                "testFirebaseUid2"
        );

        List<Notification> unread = notificationManager.getUserUnreadNotifications(admin);
        assertEquals(1, unread.size());

        notificationService.updateNotificationToRead(admin.getFirebaseUid(), unread.get(0).getNotificationId());
        Notification readNoti = notificationRepository.findById(unread.get(0).getNotificationId()).orElse(null);
        assertNotNull(readNoti);
        assertNotNull(readNoti.getReadAt());

        assertDoesNotThrow(() -> notificationService.updateNotificationToRead(
                admin.getFirebaseUid(), readNoti.getNotificationId())
        );

        Notification updateNoti = notificationRepository.findById(readNoti.getNotificationId()).orElse(null);
        assertNotNull(updateNoti);
        assertNotNull(updateNoti.getReadAt());
        assertEquals(readNoti.getReadAt(), updateNoti.getReadAt());
    }

    @Test
    @DisplayName("All-inbox page includes a notification after it is marked read")
    void testGetAllQuickNotificationsIncludesReadRows(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid", 1L, DiscussionType.COMMENT);

        reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "test",
                        ReportCategoryName.OFF_TOPIC,
                        discussionRoot.getDiscussionId()),
                "testFirebaseUid2");

        List<QuickNotificationDTO> unreadBefore =
                notificationService.getQuickNotifications(admin.getFirebaseUid());
        assertEquals(1, unreadBefore.size());

        notificationService.updateNotificationToRead(
                admin.getFirebaseUid(), unreadBefore.get(0).notificationId());

        assertTrue(notificationService.getQuickNotifications(admin.getFirebaseUid()).isEmpty());

        List<QuickNotificationDTO> allInbox =
                notificationService.getAllQuickNotifications(admin.getFirebaseUid(), 1);
        assertEquals(1, allInbox.size());
        assertEquals(unreadBefore.get(0).notificationId(), allInbox.get(0).notificationId());
        assertEquals(EventTypeName.REPORT_CREATED.name(), allInbox.get(0).summary().eventTypeName());
        assertEquals(NotificationClickKind.REPORT_QUEUE, allInbox.get(0).click().kind());
        assertNotNull(allInbox.get(0).click().reportId());
    }

    @Test
    @DisplayName("All-inbox unknown user throws")
    void testGetAllQuickNotificationsUnknownUser(){
        assertThrows(
                RuntimeException.class,
                () -> notificationService.getAllQuickNotifications("fakeUid", 1)
        );
    }

    @Test
    @DisplayName("User attempt to read an unknown notification")
    void testUserAttemptToReadUnknownNotification(){
        assertThrows(RuntimeException.class, () -> notificationService.updateNotificationToRead("testFirebaseUid", 999L));
    }
}
