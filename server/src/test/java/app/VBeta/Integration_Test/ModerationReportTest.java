package app.VBeta.Integration_Test;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.ModerationService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.ReportRepository;
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

import java.util.Optional;

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
public class ModerationReportTest {
    @Autowired
    private ModerationService moderationService;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    ClimbingProblemManager climbingProblemManager;

    @Autowired
    WallSectionManager wallSectionManager;

    @Autowired
    DiscussionRootManager discussionRootManager;

    @Autowired
    ReportManager reportManager;

    @Autowired
    private ReportRepository reportRepository;

    private final DiscussionType COMMENT = DiscussionType.COMMENT;
    private final DiscussionType BETA = DiscussionType.BETA;

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
                () -> moderationService.createNewReport(request2, user.getFirebaseUid()));

        ReportRequest request3 = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "jkadjhgjashjg",
                ReportCategoryName.HARASSMENT_BULLYING,
                discussionRoot.getDiscussionId()
        );

        ex = assertThrows(
                RuntimeException.class,
                () -> moderationService.createNewReport(request3, user.getFirebaseUid()));
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
                () -> moderationService.createNewReport(request, "12345")
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
                () -> moderationService.createNewReport(badRequest, "testFirebaseUid")
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
                () -> moderationService.createNewReport(request2, user.getFirebaseUid())
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
