package app.VBeta.Integration_Test;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.report.*;
import app.VBeta.application.ReportService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.GymRoleRepository;
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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class ModerationReportTest {
    @Autowired
    private ReportService reportService;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    ClimbingProblemManager climbingProblemManager;

    @Autowired
    WallSectionManager wallSectionManager;

    @Autowired
    DiscussionRootManager discussionRootManager;

    @Autowired
    DiscussionCommentManager discussionCommentManager;

    @Autowired
    ReportManager reportManager;

    @Autowired
    private ReportRepository reportRepository;

    private final DiscussionType COMMENT = DiscussionType.COMMENT;
    private final DiscussionType BETA = DiscussionType.BETA;
    @Autowired
    private GymRoleRepository gymRoleRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private DiscussionRootRepository discussionRootRepository;
    @Autowired
    private SolutionBetaManager solutionBetaManager;

    private UserAccount getUserAccount(String firebaseUid){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(user, String.format(
                "Expected user account with mock uid %s to be existed", firebaseUid
        ));
        return user;
    }

    private ClimbingProblem getClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        return problem;
    }

    private DiscussionRoot createDiscussionRoot(String firebaseUid, Long problemId, DiscussionType discussionType){
        UserAccount user = getUserAccount(firebaseUid);
        ClimbingProblem problem = getClimbingProblem(problemId);
        DiscussionRoot discussionRoot = discussionRootManager.createNewDiscussion(user, problem, discussionType);
        assertNotNull(discussionRoot);
        discussionRootRepository.saveAndFlush(discussionRoot);
        if (discussionType == COMMENT) {
            discussionCommentManager.storeDiscussionComment(discussionRoot, "test comment");
        } else {
            solutionBetaManager.storeUserSolutionBeta(
                    discussionRoot,
                    "testFile-" + discussionRoot.getDiscussionId() + ".mp4",
                    "https://example.test/beta/" + discussionRoot.getDiscussionId()
            );
        }
        return discussionRoot;
    }

    private void validateReport(Report report, ReportRequest request, UserAccount user, Long discussionRootId){
        assertEquals(report.getReporter(), user);
        assertEquals(report.getCategory().getCategoryName(), request.reportCategoryName());
        assertEquals(report.getTargetType(), request.reportTargetType());
        assertEquals(report.getReportReason(), request.reportReason());
        assertNull(report.getUser());
        assertNull(report.getWallSection());
        assertNull(report.getProblem());
        assertNotNull(report.getDiscussion());
        assertEquals(report.getDiscussion().getDiscussionId(), discussionRootId);
        assertNotNull(report.getCreatedAt());
        assertNull(report.getResolvedAt());
        assertEquals(ReportStatus.OPEN, report.getReportStatus());
    }

    private Report createReport(String reporterUid, String commenterUid, Long problemId, DiscussionType discussionType){
        DiscussionRoot discussion = createDiscussionRoot(commenterUid, problemId, discussionType);
        UserAccount reporter =  getUserAccount(reporterUid);
        return createDiscussionReport(reporter, discussion, ReportCategoryName.SPAM, "THIS SPAMMY");
    }

    private Report createDiscussionReport(UserAccount reporter, DiscussionRoot discussion,
                                          ReportCategoryName category, String reason) {
        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                reason,
                category,
                discussion.getDiscussionId()
        );
        Report report = reportManager.createReport(reporter, request);
        reportRepository.saveAndFlush(report);
        validateReport(report, request, reporter, discussion.getDiscussionId());
        return report;
    }

    private UserAccount createNewUser(String firebaseUid, RoleType roleType){
        GymRole role = gymRoleRepository.findByRoleType(roleType)
                .orElseThrow(() -> new RuntimeException("Incorrect role type"));
        UserAccount user = UserAccount.builder()
                .firebaseUid(firebaseUid)
                .email("testEmail@gmail.com")
                .username("testUser123")
                .gymRole(role)
                .build();
        return userAccountRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("Test success creation for generation report")
    void testSuccessReportCreation(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, COMMENT);

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "The comment is inappropriate",
                ReportCategoryName.INAPPROPRIATE_CONTENT,
                discussionRoot.getDiscussionId()
        );
        Report report = reportManager.createReport(user, request);
        assertNotNull(report);
        validateReport(report, request, user, discussionRoot.getDiscussionId());
    }

    @Test
    @DisplayName("Test for user reported their own discussion root. Expected error")
    void testUserReportTheirOwnDiscussionRoot_ExpectedError(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot(user.getFirebaseUid(), 1L, BETA);

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "I hate my own beta",
                ReportCategoryName.SPAM,
                discussionRoot.getDiscussionId()
        );
        RuntimeException exception = assertThrows(
                RuntimeException.class, () -> reportManager.createReport(user, request));
    }

    @Test
    @DisplayName("Test attempting to report a Discussion Root which already been deleted. Expected Not found")
    void testUserReportDeletedDiscussionRoot(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, BETA);
        discussionRootManager.removeDiscussion(discussionRoot);

        DiscussionRoot discuss = discussionRootManager.findDiscussionRootById(discussionRoot.getDiscussionId());
        assertNull(discuss);

        ReportRequest badRequest = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "YEYEYEYE",
                ReportCategoryName.SPAM,
                discussionRoot.getDiscussionId()
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportManager.createReport(user, badRequest)
        );
    }

    @Test
    @DisplayName("Test for not allowing user to send multiple report on the same discussion")
    void testUserReportNotAllowedToSendMultipleDiscussion(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, COMMENT);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "YEYEYE",
                ReportCategoryName.INAPPROPRIATE_CONTENT,
                discussionRoot.getDiscussionId()
        );

        Report report = reportManager.createReport(user, request1);
        assertNotNull(report);
        validateReport(report, request1, user, discussionRoot.getDiscussionId());

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "YYOYOYOYH",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportService.createNewReport(request2, user.getFirebaseUid()));

        ReportRequest request3 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "jkadjhgjashjg",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        ex = assertThrows(
                RuntimeException.class,
                () -> reportService.createNewReport(request3, user.getFirebaseUid()));
    }

    @Test
    @DisplayName("Create report with an unknown firebaseUid")
    void testCreateReportWithUnknownFirebaseUid(){

        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid", 1L, BETA);

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "1234rasdjhjsa",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportService.createNewReport(request, "12345")
        );
    }

    @Test
    @DisplayName("Create report with a wrong/not exist discussion id")
    void testCreateReportWithWrongDiscussionId(){
        ReportRequest badRequest = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "blahblahblahblahblahblah;",
                ReportCategoryName.HARASSMENT_BULLYING,
                123L
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportService.createNewReport(badRequest, "testFirebaseUid")
        );
    }

    @Test
    @DisplayName("User sending report for two diffirent discussion with the same type of report")
    void testUserGenerateMultipleReportOfSameTypeOfDiscussion(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussion1 = createDiscussionRoot("testFirebaseUid2", 1L, COMMENT);
        DiscussionRoot discussion2 = createDiscussionRoot("testFirebaseUid3", 1L, BETA);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "123e42",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussion1.getDiscussionId()
        );

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "123e42",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussion2.getDiscussionId()
        );

        Report report1 = reportManager.createReport(user, request1);
        Report report2 = reportManager.createReport(user, request2);

        assertNotNull(report1);
        assertNotNull(report2);
        validateReport(report1, request1, user, discussion1.getDiscussionId());
        validateReport(report2, request2, user, discussion2.getDiscussionId());

        assertEquals(report1.getReporter(), report2.getReporter());
        assertEquals(report1.getTargetType(), report2.getTargetType());
        assertEquals(report1.getCategory(), report2.getCategory());
        assertEquals(report1.getReportStatus(), report2.getReportStatus());
        assertEquals(report1.getReportReason(), report2.getReportReason());
    }

    @Test
    @DisplayName("Test two different users who report the same content. Expect success response")
    void testUserGenerateMultipleReportOfSameTypeOfReport(){
        UserAccount user = getUserAccount("testFirebaseUid");
        UserAccount user2 = getUserAccount("testFirebaseUid2");

        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid3", 1L, COMMENT);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Hhlashhej",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "hashdgjhsa",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        Report report1 = reportManager.createReport(user, request1);
        Report report2 = reportManager.createReport(user2, request2);

        assertNotNull(report1);
        assertNotNull(report2);
        validateReport(report1, request1, user, discussionRoot.getDiscussionId());
        validateReport(report2, request2, user2, discussionRoot.getDiscussionId());

        assertNotEquals(report1.getReporter(), report2.getReporter());
        assertEquals(report1.getTargetType(), report2.getTargetType());
        assertEquals(report1.getCategory(), report2.getCategory());
        assertEquals(report1.getReportStatus(), report2.getReportStatus());
    }

    @Test
    @DisplayName("Different user sending a second report after the first one is closed")
    void testDifferentUserSendInSecondReportAfterFirstOneIsClosed(){
        UserAccount user = getUserAccount("testFirebaseUid");
        UserAccount user2 = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, BETA);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        Report report = reportManager.createReport(user, request1);
        assertNotNull(report);
        validateReport(report, request1, user, discussionRoot.getDiscussionId());

        report.setReportStatus(ReportStatus.DISMISSED);
        reportManager.save(report);

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        Report report3 = reportManager.createReport(user2, request2);
        assertNotNull(report3);
        validateReport(report3, request2, user2, discussionRoot.getDiscussionId());

    }

    @Test
    @DisplayName("User sending a second report after the first report is dismissed")
    void testUserSendInSecondReportAfterFirstOneIsDismissed(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, BETA);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        Report report = reportManager.createReport(user, request1);
        assertNotNull(report);
        validateReport(report, request1, user, discussionRoot.getDiscussionId());

        report.setReportStatus(ReportStatus.DISMISSED);
        reportRepository.saveAndFlush(report);

        Optional<Report> reportTest = reportRepository.findById(report.getReportId());
        assertTrue(reportTest.isPresent());
        assertEquals(ReportStatus.DISMISSED, reportTest.get().getReportStatus());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportService.createNewReport(request2, user.getFirebaseUid())
        );

    }

    @Test
    @DisplayName("User sending a second report after the first one is dismissed under different category")
    void testUserSendInSecondReportAfterFirstOneIsDismissedUnderDifferentCategory(){
        UserAccount user = getUserAccount("testFirebaseUid");
        DiscussionRoot discussionRoot = createDiscussionRoot("testFirebaseUid2", 1L, COMMENT);

        ReportRequest request1 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.OFF_TOPIC,
                discussionRoot.getDiscussionId()
        );

        Report report = reportManager.createReport(user, request1);
        assertNotNull(report);
        validateReport(report, request1, user, discussionRoot.getDiscussionId());

        report.setReportStatus(ReportStatus.DISMISSED);
        reportRepository.saveAndFlush(report);

        ReportRequest request2 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "test",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        Report report2 = reportManager.createReport(user, request2);
        assertNotNull(report2);
        validateReport(report2, request2, user, discussionRoot.getDiscussionId());
    }

    @Test
    @DisplayName("User can report a climbing problem")
    void testSuccessReportClimbingProblem(){
        UserAccount user = getUserAccount("testFirebaseUid");
        ClimbingProblem problem = getClimbingProblem(1L);

        ReportRequest request = new ReportRequest(
                ReportTargetType.CLIMBING_PROBLEM,
                "Holds are misleading",
                ReportCategoryName.INAPPROPRIATE_CONTENT,
                problem.getId()
        );
        Report report = reportManager.createReport(user, request);
        assertNotNull(report);
        validateTypedReport(report, request, user);
    }

    @Test
    @DisplayName("User can report a wall section")
    void testSuccessReportWallSection(){
        UserAccount user = getUserAccount("testFirebaseUid");
        WallSection wallSection = wallSectionManager.findWallSection(1L);

        ReportRequest request = new ReportRequest(
                ReportTargetType.WALL_SECTION,
                "Section info is wrong",
                ReportCategoryName.OFF_TOPIC,
                wallSection.getId()
        );
        Report report = reportManager.createReport(user, request);
        assertNotNull(report);
        validateTypedReport(report, request, user);
    }

    @Test
    @DisplayName("User can report another user account")
    void testSuccessReportUserAccount(){
        UserAccount reporter = getUserAccount("testFirebaseUid");
        UserAccount reported = getUserAccount("testFirebaseUid2");

        ReportRequest request = new ReportRequest(
                ReportTargetType.USER_ACCOUNT,
                "Harassment in DMs",
                ReportCategoryName.HARASSMENT_BULLYING,
                reported.getId()
        );
        Report report = reportManager.createReport(reporter, request);
        assertNotNull(report);
        validateTypedReport(report, request, reporter);
    }

    @Test
    @DisplayName("User cannot report their own account")
    void testUserCannotReportOwnAccount(){
        UserAccount user = getUserAccount("testFirebaseUid");

        ReportRequest request = new ReportRequest(
                ReportTargetType.USER_ACCOUNT,
                "Reporting myself",
                ReportCategoryName.SPAM,
                user.getId()
        );
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> reportManager.createReport(user, request)
        );
    }

    @Test
    @DisplayName("Missing climbing problem, wall section, and user targets return 404")
    void testMissingNonDiscussionTargetsReturnNotFound(){
        UserAccount user = getUserAccount("testFirebaseUid");

        RuntimeException problemEx = assertThrows(
                RuntimeException.class,
                () -> reportManager.createReport(user, new ReportRequest(
                        ReportTargetType.CLIMBING_PROBLEM,
                        "missing problem",
                        ReportCategoryName.SPAM,
                        999_999L))
        );
        RuntimeException wallEx = assertThrows(
                RuntimeException.class,
                () -> reportManager.createReport(user, new ReportRequest(
                        ReportTargetType.WALL_SECTION,
                        "missing wall",
                        ReportCategoryName.SPAM,
                        999_999L))
        );
        RuntimeException userEx = assertThrows(
                RuntimeException.class,
                () -> reportManager.createReport(user, new ReportRequest(
                        ReportTargetType.USER_ACCOUNT,
                        "missing user",
                        ReportCategoryName.SPAM,
                        999_999L))
        );
    }

    @Test
    @DisplayName("Success getting report queue list for admin")
    void testSuccessGetReportQueueListForAdmin(){
        UserAccount user1 = getUserAccount("testFirebaseUid");
        UserAccount user2 = getUserAccount("testFirebaseUid2");
        Report report = createReport(user1.getFirebaseUid(), user2.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        UserAccount admin = getUserAccount("testFirebaseUid3");

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(report));
    }

    @Test
    @DisplayName("Fail getting report queue due to incorrect role")
    void testFailGetReportQueueListForNotAdmin(){
        UserAccount user = getUserAccount("testFirebaseUid");
        assertThrows(RuntimeException.class, () -> reportService.getReportQueue(user.getFirebaseUid()));
    }

    @Test
    @DisplayName("Success getting empty Report queue for Admin when nothing is reported")
    void testSuccessGetReportQueueListForAdminWhenNothingIsReported(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of());
    }

    @Test
    @DisplayName("Success hide report from admin if admin is the one the report content target")
    void testSuccessHideReportFromAdminIfAdminIsTheOneTheReportContentTarget(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount reporter = getUserAccount("testFirebaseUid");
        Report report = createReport(reporter.getFirebaseUid(), admin.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        assertNotNull(payload);
        assertEquals(0, payload.reports().size());
    }

    @Test
    @DisplayName("Report only show up to admin that not from the report target")
    void testSuccessReportShowUpForAdminNotFromTheReportTarget(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount reporter = getUserAccount("testFirebaseUid");
        UserAccount admin2 = createNewUser("testAdminUser", RoleType.ADMIN);

        Report report = createReport(reporter.getFirebaseUid(), admin.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        assertNotNull(payload);
        assertEquals(0, payload.reports().size());

        ReportsPayload payload2 = reportService.getReportQueue(admin2.getFirebaseUid());
        assertNotNull(payload2);
        assertEquals(1, payload2.reports().size());
        validateReportsPayload(payload2, List.of(report));
    }

    @Test
    @DisplayName("Queue groups the same target when reporters use different categories")
    void testGetReportQueueGroupsSameTargetByCategory(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);

        Report spam = createDiscussionReport(climber, discussion, ReportCategoryName.SPAM, "spam");
        Report harassment = createDiscussionReport(
                admin, discussion, ReportCategoryName.HARASSMENT_BULLYING, "harassment");

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(spam, harassment));
    }

    @Test
    @DisplayName("Queue lists multiple reported targets")
    void testGetReportQueueListsMultipleTargets(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        Report first = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        Report second = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(first, second));
    }

    @Test
    @DisplayName("Success getting ReportPayLoad from report id")
    void testSuccessGetReportPayLoadFromReportId(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");


        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), report.getReportId());
        validateReportsPayload(payload, List.of(report));
    }

    @Test
    @DisplayName("Success getting multiple report from report id")
    void testSuccessGetMultipleReportsFromReportId(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);

        Report spam = createDiscussionReport(climber, discussion, ReportCategoryName.SPAM, "spam");
        Report harassment = createDiscussionReport(
                admin, discussion, ReportCategoryName.HARASSMENT_BULLYING, "harassment");

        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), spam.getReportId());
        validateReportsPayload(payload, List.of(spam, harassment));
    }

    @Test
    @DisplayName("Fail to get report from wrong report id")
    void testFailedToGetReportFromWrongReportId(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        assertThrows(RuntimeException.class, () -> reportService.getReport(admin.getFirebaseUid(), 123214L));
    }

    @Test
    @DisplayName("Success hide report from admin with report id when the reported is for admin user")
    void testSuccessHideReportFromAdminWithReportId(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        Report report = createReport(setter.getFirebaseUid(), admin.getFirebaseUid(), 1L, COMMENT);

        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), report.getReportId());
        assertNotNull(payload);
        assertEquals(0, payload.reports().size());
    }

    @Test
    @DisplayName("Success get report from admin wiht report id for admin not in report target")
    void testSuccessGetReportFromAdminNotInReportTarget(){
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount reporter = getUserAccount("testFirebaseUid");
        UserAccount admin2 = createNewUser("testAdminUser", RoleType.ADMIN);

        Report report = createReport(reporter.getFirebaseUid(), admin.getFirebaseUid(), 1L, DiscussionType.COMMENT);

        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), report.getReportId());
        assertNotNull(payload);
        assertEquals(0, payload.reports().size());

        ReportsPayload payload2 = reportService.getReport(admin2.getFirebaseUid(), report.getReportId());
        assertNotNull(payload2);
        assertEquals(1, payload2.reports().size());
        validateReportsPayload(payload2, List.of(report));
    }

    @Test
    @DisplayName("Admin queue includes a reported beta with a discussion snapshot")
    void testSuccessReportBetaAppearsInAdminQueue(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, BETA);
        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(report));
        assertEquals(DiscussionType.BETA, payload.reports().get(0).report().discussion().discussionType());
    }

    @Test
    @DisplayName("Queue orders discussion cases by descending queue score")
    void testGetReportQueueOrdersHigherScoreFirst(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        DiscussionRoot offTopicDiscussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);
        DiscussionRoot inappropriateDiscussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);
        Report offTopic = createDiscussionReport(
                climber, offTopicDiscussion, ReportCategoryName.OFF_TOPIC, "off topic");
        Report inappropriate = createDiscussionReport(
                climber, inappropriateDiscussion, ReportCategoryName.INAPPROPRIATE_CONTENT, "inappropriate");

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(offTopic, inappropriate));
        assertEquals(
                inappropriate.getDiscussion().getDiscussionId(),
                payload.reports().get(0).report().discussion().discussionId()
        );
        assertTrue(payload.reports().get(0).queueScore() > payload.reports().get(1).queueScore());
    }

    @Test
    @DisplayName("Same discussion and category from two reporters multiplies category score")
    void testGetReportQueueSameCategoryTwoReportersIncreasesScore(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);

        Report first = createDiscussionReport(
                climber, discussion, ReportCategoryName.HARASSMENT_BULLYING, "harassment one");
        Report second = createDiscussionReport(
                admin, discussion, ReportCategoryName.HARASSMENT_BULLYING, "harassment two");

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(first, second));

        ReportPriorityDTO caseDto = payload.reports().get(0);
        assertEquals(1, payload.reports().size());
        assertEquals(6, caseDto.queueScore());
        assertEquals(1, caseDto.categories().size());
        assertEquals(ReportCategoryName.HARASSMENT_BULLYING, caseDto.categories().get(0).categoryName());
        assertEquals(2, caseDto.categories().get(0).reportCount());
        assertEquals(6, caseDto.categories().get(0).categoryScore());
    }

    @Test
    @DisplayName("Dismissed reports are excluded from the admin queue")
    void testGetReportQueueExcludesDismissedReports(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        Report openReport = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        Report dismissedReport = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        dismissedReport.setReportStatus(ReportStatus.DISMISSED);
        reportRepository.saveAndFlush(dismissedReport);

        ReportsPayload payload = reportService.getReportQueue(admin.getFirebaseUid());
        validateReportsPayload(payload, List.of(openReport));
    }

    @Test
    @DisplayName("Hide an admin-owned beta from that admin's queue and detail")
    void testHideAdminOwnedBetaFromQueueAndDetail(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        UserAccount otherAdmin = createNewUser("testAdminUserBeta", RoleType.ADMIN);

        Report report = createReport(climber.getFirebaseUid(), admin.getFirebaseUid(), 1L, BETA);

        ReportsPayload ownerQueue = reportService.getReportQueue(admin.getFirebaseUid());
        assertEquals(0, ownerQueue.reports().size());
        ReportsPayload ownerDetail = reportService.getReport(admin.getFirebaseUid(), report.getReportId());
        assertEquals(0, ownerDetail.reports().size());

        ReportsPayload otherQueue = reportService.getReportQueue(otherAdmin.getFirebaseUid());
        validateReportsPayload(otherQueue, List.of(report));
        ReportsPayload otherDetail = reportService.getReport(otherAdmin.getFirebaseUid(), report.getReportId());
        validateReportsPayload(otherDetail, List.of(report));
    }

    @Test
    @DisplayName("Setter cannot view the report queue")
    void testSetterCannotGetReportQueue(){
        UserAccount setter = getUserAccount("testFirebaseUid2");
        assertThrows(RuntimeException.class, () -> reportService.getReportQueue(setter.getFirebaseUid()));
    }

    @Test
    @DisplayName("Setter cannot view a report by id")
    void testSetterCannotGetReportById(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        assertThrows(RuntimeException.class,
                () -> reportService.getReport(setter.getFirebaseUid(), report.getReportId()));
    }

    @Test
    @DisplayName("Climber cannot view a report by id")
    void testClimberCannotGetReportById(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        assertThrows(RuntimeException.class,
                () -> reportService.getReport(climber.getFirebaseUid(), report.getReportId()));
    }

    @Test
    @DisplayName("Unknown firebase uid cannot view the report queue")
    void testGetReportQueueUnknownFirebaseUid(){
        assertThrows(RuntimeException.class, () -> reportService.getReportQueue("unknown-firebase-uid"));
    }

    @Test
    @DisplayName("Unknown firebase uid cannot view a report by id")
    void testGetReportByIdUnknownFirebaseUid(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        assertThrows(RuntimeException.class,
                () -> reportService.getReport("unknown-firebase-uid", report.getReportId()));
    }

    @Test
    @DisplayName("Get by id returns empty when the report is dismissed and has no open siblings")
    void testGetReportByIdDismissedReturnsEmpty(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");

        Report report = createReport(climber.getFirebaseUid(), setter.getFirebaseUid(), 1L, COMMENT);
        report.setReportStatus(ReportStatus.DISMISSED);
        reportRepository.saveAndFlush(report);

        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), report.getReportId());
        assertNotNull(payload);
        assertEquals(0, payload.reports().size());
    }

    @Test
    @DisplayName("Get by id returns remaining open siblings after one report on the discussion is dismissed")
    void testGetReportByIdReturnsRemainingOpenSiblingsWhenOneDismissed(){
        UserAccount climber = getUserAccount("testFirebaseUid");
        UserAccount setter = getUserAccount("testFirebaseUid2");
        UserAccount admin = getUserAccount("testFirebaseUid3");
        DiscussionRoot discussion = createDiscussionRoot(setter.getFirebaseUid(), 1L, COMMENT);

        Report dismissed = createDiscussionReport(climber, discussion, ReportCategoryName.SPAM, "spam");
        Report stillOpen = createDiscussionReport(
                admin, discussion, ReportCategoryName.HARASSMENT_BULLYING, "harassment");
        dismissed.setReportStatus(ReportStatus.DISMISSED);
        reportRepository.saveAndFlush(dismissed);

        ReportsPayload payload = reportService.getReport(admin.getFirebaseUid(), dismissed.getReportId());
        validateReportsPayload(payload, List.of(stillOpen));
    }

    private void validateReportsPayload(ReportsPayload payload, List<Report> expectedReports) {
        assertNotNull(payload);
        assertNotNull(payload.reports());

        Map<QueueTargetKey, List<Report>> expectedByTarget = expectedReports.stream()
                .collect(Collectors.groupingBy(this::queueTargetKey, LinkedHashMap::new, Collectors.toList()));
        assertEquals(expectedByTarget.size(), payload.reports().size());

        List<Integer> scores = payload.reports().stream()
                .map(ReportPriorityDTO::queueScore)
                .toList();
        List<Integer> sortedScores = scores.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        assertEquals(sortedScores, scores);

        Map<QueueTargetKey, ReportPriorityDTO> actualByTarget = payload.reports().stream()
                .collect(Collectors.toMap(this::queueTargetKey, dto -> dto));

        expectedByTarget.forEach((target, reportsOnTarget) -> {
            ReportPriorityDTO actual = actualByTarget.get(target);
            assertNotNull(actual, () -> "Missing queue case for " + target);
            validateReportPriorityDTO(actual, reportsOnTarget);
        });
    }

    private void validateReportPriorityDTO(ReportPriorityDTO priorityDTO, List<Report> reportsOnTarget) {
        assertNotNull(priorityDTO);

        Map<ReportCategoryName, List<Report>> expectedByCategory = reportsOnTarget.stream()
                .collect(Collectors.groupingBy(report -> report.getCategory().getCategoryName()));
        assertEquals(expectedByCategory.size(), priorityDTO.categories().size());

        Map<ReportCategoryName, CategoryTallyDTO> actualTallies = priorityDTO.categories().stream()
                .collect(Collectors.toMap(CategoryTallyDTO::categoryName, tally -> tally));

        int expectedScore = 0;
        for (Map.Entry<ReportCategoryName, List<Report>> entry : expectedByCategory.entrySet()) {
            CategoryTallyDTO tally = actualTallies.get(entry.getKey());
            assertNotNull(tally, () -> "Missing category tally " + entry.getKey());
            int count = entry.getValue().size();
            int weight = entry.getValue().get(0).getCategory().getWeight();
            assertEquals(count, tally.reportCount());
            assertEquals(weight * count, tally.categoryScore());
            expectedScore += weight * count;
        }
        assertEquals(expectedScore, priorityDTO.queueScore());
        validateReportDTO(reportsOnTarget, priorityDTO.report());
    }

    private void validateReportDTO(List<Report> reportsOnTarget, ReportDTO reportDTO) {
        assertNotNull(reportDTO);
        Report first = reportsOnTarget.get(0);
        assertEquals(first.getTargetType(), reportDTO.targetType());
        validateTargetSnapshot(first, reportDTO);
        assertEquals(reportsOnTarget.size(), reportDTO.reporters().size());

        Map<Long, ReportUserDTO> reportersById = reportDTO.reporters().stream()
                .collect(Collectors.toMap(ReportUserDTO::reportId, reporter -> reporter));
        for (Report report : reportsOnTarget) {
            ReportUserDTO reporterDto = reportersById.get(report.getReportId());
            assertNotNull(reporterDto, () -> "Missing reporter row for report " + report.getReportId());
            validateReportUserDTO(
                    reporterDto,
                    report.getReporter(),
                    report,
                    report.getCategory().getCategoryName()
            );
        }
    }

    private void validateTargetSnapshot(Report report, ReportDTO reportDTO) {
        switch (report.getTargetType()) {
            case DISCUSSION -> {
                assertNotNull(reportDTO.discussion());
                assertEquals(report.getDiscussion().getDiscussionId(), reportDTO.discussion().discussionId());
                ClimbingProblem problem = report.getDiscussion().getProblem();
                assertNotNull(problem);
                assertNotNull(reportDTO.climbingProblem());
                assertEquals(problem.getId(), reportDTO.climbingProblem().problemId());
                WallSection wall = problem.getWallSection();
                assertNotNull(wall);
                assertNotNull(reportDTO.wallSection());
                assertEquals(wall.getId(), reportDTO.wallSection().wallSectionID());
                assertNull(reportDTO.user());
            }
            case CLIMBING_PROBLEM -> {
                assertNotNull(reportDTO.climbingProblem());
                assertEquals(report.getProblem().getId(), reportDTO.climbingProblem().problemId());
                WallSection wall = report.getProblem().getWallSection();
                assertNotNull(wall);
                assertNotNull(reportDTO.wallSection());
                assertEquals(wall.getId(), reportDTO.wallSection().wallSectionID());
                assertNull(reportDTO.discussion());
                assertNull(reportDTO.user());
            }
            case WALL_SECTION -> {
                assertNotNull(reportDTO.wallSection());
                assertEquals(report.getWallSection().getId(), reportDTO.wallSection().wallSectionID());
                assertNull(reportDTO.discussion());
                assertNull(reportDTO.climbingProblem());
                assertNull(reportDTO.user());
            }
            case USER_ACCOUNT -> {
                assertNotNull(reportDTO.user());
                assertEquals(report.getUser().getId(), reportDTO.user().userId());
                assertNull(reportDTO.discussion());
                assertNull(reportDTO.climbingProblem());
                assertNull(reportDTO.wallSection());
            }
        }
    }

    private record QueueTargetKey(ReportTargetType targetType, Long targetId) {}

    private QueueTargetKey queueTargetKey(Report report) {
        Long targetId = switch (report.getTargetType()) {
            case DISCUSSION -> report.getDiscussion().getDiscussionId();
            case CLIMBING_PROBLEM -> report.getProblem().getId();
            case WALL_SECTION -> report.getWallSection().getId();
            case USER_ACCOUNT -> report.getUser().getId();
        };
        return new QueueTargetKey(report.getTargetType(), targetId);
    }

    private QueueTargetKey queueTargetKey(ReportPriorityDTO priorityDTO) {
        ReportDTO report = priorityDTO.report();
        Long targetId = switch (report.targetType()) {
            case DISCUSSION -> report.discussion().discussionId();
            case CLIMBING_PROBLEM -> report.climbingProblem().problemId();
            case WALL_SECTION -> report.wallSection().wallSectionID();
            case USER_ACCOUNT -> report.user().userId();
        };
        return new QueueTargetKey(report.targetType(), targetId);
    }

    private void validateReportUserDTO(ReportUserDTO reportUserDTO, UserAccount user,
                                       Report report, ReportCategoryName category){
        assertNotNull(reportUserDTO);
        assertEquals(report.getReportId(), reportUserDTO.reportId());
        assertEquals(category, reportUserDTO.categoryName());
        assertEquals(report.getReportReason(), reportUserDTO.reportReason());
        assertEquals(report.getCreatedAt(), reportUserDTO.createdAt());

        UserAccountDTO reporter =  reportUserDTO.reporter();
        assertNotNull(reporter);
        assertEquals(user.getEmail(), reporter.email());
        assertEquals(user.getUsername(), reporter.username());
        assertEquals(user.getGymRole().getRoleType().name(), reporter.role());
        assertEquals(user.getId(), reporter.userId());
    }

    private void validateTypedReport(Report report, ReportRequest request, UserAccount reporter){
        assertEquals(reporter, report.getReporter());
        assertEquals(request.reportCategoryName(), report.getCategory().getCategoryName());
        assertEquals(request.reportTargetType(), report.getTargetType());
        assertEquals(request.reportReason(), report.getReportReason());
        assertNotNull(report.getCreatedAt());
        assertNull(report.getResolvedAt());
        assertEquals(ReportStatus.OPEN, report.getReportStatus());

        switch (request.reportTargetType()) {
            case CLIMBING_PROBLEM -> {
                assertNotNull(report.getProblem());
                assertEquals(request.targetId(), report.getProblem().getId());
                assertNull(report.getDiscussion());
                assertNull(report.getWallSection());
                assertNull(report.getUser());
            }
            case WALL_SECTION -> {
                assertNotNull(report.getWallSection());
                assertEquals(request.targetId(), report.getWallSection().getId());
                assertNull(report.getDiscussion());
                assertNull(report.getProblem());
                assertNull(report.getUser());
            }
            case USER_ACCOUNT -> {
                assertNotNull(report.getUser());
                assertEquals(request.targetId(), report.getUser().getId());
                assertNull(report.getDiscussion());
                assertNull(report.getProblem());
                assertNull(report.getWallSection());
            }
            default -> fail("Expected a non-discussion report target");
        }
    }
}
