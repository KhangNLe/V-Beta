package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.DiscussionContentReplyRequest;
import app.VBeta.api.dto.discussions.DiscussionContentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.discussions.comment.CommentDeletionRequest;
import app.VBeta.api.dto.problems.ClimbingProblemCreationRequest;
import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.application.DiscussionReplyService;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.application.ReportService;
import app.VBeta.application.support.discussion.beta.SolutionBetaManager;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.application.support.events.NotificationManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.config.TestGcpStorageConfig;
import app.VBeta.repository.DiscussionRootRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class DiscussionReplyTest {
    @Autowired
    private DiscussionRootManager discussionRootManager;

    @Autowired
    private ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    @Autowired
    private DiscussionCommentManager discussionCommentManager;

    @Autowired
    private SolutionBetaManager solutionBetaManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private WallSectionManager wallSectionManager;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private NotificationManager notificationManager;

    private final String USER1ID = "testFirebaseUid";
    private final String USER2ID = "testFirebaseUid2";
    private final String USER_DELETED_OWN_DISCUSSION = "User deleted their own discussion";

    @Autowired
    private DiscussionReplyService discussionReplyService;

    @Autowired
    private ProblemDiscussionService problemDiscussionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;


    private DiscussionRoot createNewDiscussion(ClimbingProblem problem, UserAccount author,
                                               DiscussionContentRequest request){
        DiscussionRoot root;
        if (request.commentInfo() == null && request.objectFileName() == null && request.videoURL() == null){
            root = null;
        } else if (request.commentInfo() == null){
            root = discussionRootManager.createNewDiscussion(author, problem, DiscussionType.BETA);
            solutionBetaManager.storeUserSolutionBeta(root, request.objectFileName(), request.videoURL());
        } else if (request.objectFileName() == null && request.videoURL() == null){
            root = discussionRootManager.createNewDiscussion(author, problem, DiscussionType.COMMENT);
            discussionCommentManager.storeDiscussionComment(root, request.commentInfo());
        } else {
            root = discussionRootManager.createNewDiscussion(author, problem, DiscussionType.MIXED);
            solutionBetaManager.storeUserSolutionBeta(root, request.objectFileName(), request.videoURL());
            discussionCommentManager.storeDiscussionComment(root, request.commentInfo());
        }
        assertNotNull(root);
        return root;
    }

    private ClimbingProblem createNewProblem(){
        WallSection wall = wallSectionManager.createNewWallSection(new WallSectionCreationRequest(
                "BIG WALL",
                "Test wall",
                null,
                null
        ));
        assertNotNull(wall);
        ClimbingProblem problem = climbingProblemManager.generateNewClimbingProblem(wall,
                new ClimbingProblemCreationRequest(
                "Test Color",
                        "This is a test problem",
                        GradeDefinition.V0,
                        null,
                        null
        ));
        assertNotNull(problem);
        return problem;
    }

    @Test
    @DisplayName("Test for creating a reply comment to an existing problem successfully")
    void testSuccessfullyCreatingAReplyCommentToADiscussion(){
        UserAccount user1 = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        List<Notification> userNoti = notificationManager.getUserUnreadNotifications(user1);
        assertTrue(userNoti.isEmpty());

        DiscussionContentRequest discussionRequest = new DiscussionContentRequest(
                problem.getId(),
                "Pretty cool problem",
                "fake_object.mp4",
                "https://www.fakevidmp4.mp3"
        );

        DiscussionRoot root = createNewDiscussion(problem, user1, discussionRequest);

        DiscussionContentReplyRequest request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user1.getId(),
                "Very insightful comment dawg",
                null,
                null
        );

        UserDiscussionData response = discussionReplyService.storeUserReply(USER2ID, request);
        userNoti = notificationManager.getUserUnreadNotifications(user1);

        assertNotNull(response);
        assertEquals(request.commentInfo(), response.comment());
        assertNull(response.videoURL());
        assertEquals(user2.getId(), response.userId());
        assertEquals(user2.getUsername(), response.username());
        assertEquals(root.getDiscussionId(), response.parentDiscussionId());
        assertEquals(1, userNoti.size());
    }

    @Test
    @DisplayName("Test for creating a reply solution beta to an existing problem successfully")
    void testSuccessfullyCreatingAReplySolutionBetaToADiscussion(){
        UserAccount user1 = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionRequest = new DiscussionContentRequest(
                problem.getId(),
                "IDK how to do this",
                null,
                null
        );

        DiscussionRoot root = createNewDiscussion(problem, user1, discussionRequest);
        DiscussionContentReplyRequest request = new DiscussionContentReplyRequest(
                 root.getDiscussionId(),
                 problem.getId(),
                 user1.getId(),
                 null,
                 "fake_video.mov",
                 "http://fakevids.com"
         );

         UserDiscussionData response = discussionReplyService.storeUserReply(USER2ID, request);

         assertNotNull(response);
         assertNull(response.comment());
         assertNotNull(response.videoURL());
         assertEquals(user2.getId(), response.userId());
         assertEquals(user2.getUsername(), response.username());
         assertEquals(root.getDiscussionId(), response.parentDiscussionId());
         assertEquals(request.videoURL(), response.videoURL());
    }

    @Test
    @DisplayName("Test of creating a mixed reply to an existing problem successfully")
    void testSuccessfullyCreatingAReplyMixedToADiscussion(){
        UserAccount user1 = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionRequest = new DiscussionContentRequest(
                problem.getId(),
                "Here's the beta for this prob",
                "fake_beta_vids.mp4",
                "http://fakebetavid.com"
        );

        DiscussionRoot root = createNewDiscussion(problem, user1, discussionRequest);

        DiscussionContentReplyRequest request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user1.getId(),
                "Here is another way of doing it",
                "another_fake_vid.mp4",
                "http://anotherfakevid.com"
        );

        UserDiscussionData response = discussionReplyService.storeUserReply(USER2ID, request);

        assertNotNull(response);
        assertNotNull(response.comment());
        assertNotNull(response.videoURL());
        assertEquals(user2.getId(), response.userId());
        assertEquals(user2.getUsername(), response.username());
        assertEquals(root.getDiscussionId(), response.parentDiscussionId());
        assertEquals(request.videoURL(), response.videoURL());
        assertEquals(request.commentInfo(), response.comment());
    }

    @Test
    @DisplayName("Test for failing reply from missing parent discussion id")
    void testFailReplyForMissingParentId(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);

        ClimbingProblem problem = createNewProblem();

        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                null,
                problem.getId(),
                user.getId(),
                "test",
                "testing.mp4",
                "http://testing.mp5"
        );

        assertThrows(RuntimeException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Test for failing reply to wrong problem id")
    void testFailReplyForWrongProblemId(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest  discussionRequest = new DiscussionContentRequest(
                problem.getId(),
                "test",
                null,
                null
        );
        DiscussionRoot root = createNewDiscussion(problem, user, discussionRequest);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                9999L,
                user.getId(),
                "yehyeh",
                null,
                null
        );

        assertThrows(RuntimeException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }
    
    @Test
    @DisplayName("Test for failing when replying to an archive problem")
    void testFailReplyForArchiveProblem(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();
        DiscussionContentRequest discussionRequest = new DiscussionContentRequest(
                problem.getId(),
                "Cool stuffs",
                "coolbeta.mp4",
                "http://coolbeta.mp4"
        );

        DiscussionRoot root =  createNewDiscussion(problem, user, discussionRequest);
        climbingProblemManager.archiveActiveProblems(List.of(problem));
        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "cool stuffs",
                null,
                null
        );

        assertThrows(RuntimeException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Test for replying to the correct parent root id but wrong user ")
    void testFailReplyToWrongUserWithCorrectParentRoot(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount user3 = userAccountManager.findUserAccount("testFirebaseUid3");
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "Cool stuffs",
                null,
                null
        );
        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);
        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user3.getId(),
                "very cool stuffs",
                null,
                null
        );
        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Test for failing due to reply request contains no comment nor solution beta")
    void testFailReplyWithNoContent(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                null,
                "fakevid.mp8",
                "http://fakevid.mp8"
        );

        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);
        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Test fail reply due to either object file is missing or video url is missing")
    void testFailReplyWithMissingObjectFileOrVideoURL(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "Cool stuffs",
                null,
                null
        );

        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);
        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "Check this out",
                "fakevid.mp5",
                null
        );

        DiscussionContentReplyRequest  request2 = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "Check this out",
                null,
                "http://fakevid.mp5"
        );

        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID, request2));
    }

    @Test
    @DisplayName("Test successfully making multiple reply to a discussion")
    void testSuccessfullyCreateMultipleReply(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "cool stuffs",
                "test",
                "fakewebsite.com"
        );

        DiscussionRoot root =  createNewDiscussion(problem, user, discussionContentRequest);

        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "very cool",
                null,
                null
        );

        DiscussionContentReplyRequest  request2 = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                null,
                "fakevid",
                "superfakewebsite.com"
        );

        UserDiscussionData response1 = discussionReplyService.storeUserReply(USER2ID, request);
        UserDiscussionData response2 = discussionReplyService.storeUserReply(USER2ID, request2);

        assertNotNull(response1);
        assertNotNull(response2);
        assertNull(response1.videoURL());
        assertNull(response2.comment());
        assertEquals(root.getDiscussionId(), response1.parentDiscussionId());
        assertEquals(root.getDiscussionId(), response2.parentDiscussionId());
        assertNotEquals(response1.discussionId(), response2.discussionId());
    }

    @Test
    @DisplayName("Test failing for posting multiple duplicate reply")
    void testFailingForPostingMultipleReply(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();
        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "check this out",
                "fakevid.mp4",
                "fakewebsite.com"
        );

        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);

        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "very cool",
                null,
                null
        );

        UserDiscussionData response = discussionReplyService.storeUserReply(USER2ID, request);

        assertNotNull(response);
        assertEquals(request.commentInfo(), response.comment());
        assertNull(response.videoURL());
        assertEquals(user2.getId(), response.userId());
        assertEquals(user2.getUsername(), response.username());
        assertEquals(root.getDiscussionId(), response.parentDiscussionId());
        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Test for having a reply to a user reply")
    void testSuccessfullyReplyToUserReply(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "Check this out",
                "fakevib.mp4",
                "fakewebsite.com"
        );

        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);

        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "very cool dawg",
                null,
                null
        );

        UserDiscussionData user2Reply = discussionReplyService.storeUserReply(USER2ID, request);

        assertNotNull(user2Reply);
        assertEquals(request.commentInfo(), user2Reply.comment());
        assertEquals(root.getDiscussionId(), user2Reply.parentDiscussionId());
        assertEquals(1, notificationManager.getUserUnreadNotifications(user).size());
        assertTrue(notificationManager.getUserUnreadNotifications(user2).isEmpty());

        DiscussionContentReplyRequest request2 =  new DiscussionContentReplyRequest(
                user2Reply.discussionId(),
                problem.getId(),
                user2.getId(),
                "thanks :)",
                null,
                null
        );

        UserDiscussionData user1Reply = discussionReplyService.storeUserReply(USER1ID, request2);
        assertNotNull(user1Reply);
        assertEquals(request2.commentInfo(), user1Reply.comment());
        assertEquals(user2Reply.discussionId(), user1Reply.parentDiscussionId());

        List<Notification> user2Notis = notificationManager.getUserUnreadNotifications(user2);
        assertEquals(1, user2Notis.size());
        assertEquals(EventTypeName.DISCUSSION_REPLY,
                user2Notis.get(0).getEvent().getEventType().getEventTypeName());
        assertEquals(1, notificationManager.getUserUnreadNotifications(user).size());
    }

    @Test
    @DisplayName("Test successfully having multiple reply to the same discussion")
    void testSuccessfullyMultipleReplyToDiscussion(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        List<Notification> user1Notis = notificationManager.getUserUnreadNotifications(user);
        List<Notification> user2Notis = notificationManager.getUserUnreadNotifications(user2);

        assertTrue(user1Notis.isEmpty());
        assertTrue(user2Notis.isEmpty());

        DiscussionContentRequest discussionContentRequest =  new DiscussionContentRequest(
                problem.getId(),
                "here's a comment",
                null,
                null
        );

        DiscussionRoot root = createNewDiscussion(problem, user, discussionContentRequest);

        DiscussionContentReplyRequest  request = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "very cool",
                null,
                null
        );

        DiscussionContentReplyRequest  request2 = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "here is another one",
                null,
                null
        );

        DiscussionContentReplyRequest request3 = new DiscussionContentReplyRequest(
                root.getDiscussionId(),
                problem.getId(),
                user.getId(),
                "Replying to myself",
                null,
                null
        );

        UserDiscussionData reply1 = discussionReplyService.storeUserReply(USER2ID, request);
        UserDiscussionData reply2 = discussionReplyService.storeUserReply(USER2ID, request2);
        UserDiscussionData reply3 = discussionReplyService.storeUserReply(USER1ID, request3);

        user1Notis = notificationManager.getUserUnreadNotifications(user);
        user2Notis = notificationManager.getUserUnreadNotifications(user2);

        assertNotNull(reply1);
        assertNotNull(reply2);
        assertNotNull(reply3);
        assertEquals(request.commentInfo(), reply1.comment());
        assertEquals(request2.commentInfo(), reply2.comment());
        assertEquals(request3.commentInfo(), reply3.comment());
        assertEquals(root.getDiscussionId(), reply1.parentDiscussionId());
        assertEquals(root.getDiscussionId(), reply2.parentDiscussionId());
        assertEquals(root.getDiscussionId(), reply3.parentDiscussionId());
        assertEquals(user2.getId(), reply1.userId());
        assertEquals(user2.getId(), reply2.userId());
        assertEquals(user.getId(), reply3.userId());
        assertEquals(2, user1Notis.size());
        assertEquals(0, user2Notis.size());
    }

    @Test
    @DisplayName("Self-reply to own discussion does not create a notification")
    void testSelfReplyDoesNotNotify(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent post", null, null));

        assertTrue(notificationManager.getUserUnreadNotifications(user).isEmpty());

        UserDiscussionData reply = discussionReplyService.storeUserReply(USER1ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Replying to myself", null, null));

        assertNotNull(reply);
        assertEquals(root.getDiscussionId(), reply.parentDiscussionId());
        assertTrue(notificationManager.getUserUnreadNotifications(user).isEmpty());
    }

    @Test
    @DisplayName("Problem timeline includes replies with parentDiscussionId")
    void testTimelineIncludesRepliesWithParentDiscussionId(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent comment", null, null));

        UserDiscussionData reply = discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Child reply", null, null));

        List<UserDiscussionData> timeline = climbingProblemDiscussionManager.getCommentsForProblem(problem);

        UserDiscussionData parentItem = timeline.stream()
                .filter(item -> item.discussionId().equals(root.getDiscussionId()))
                .findFirst()
                .orElseThrow();
        UserDiscussionData replyItem = timeline.stream()
                .filter(item -> item.discussionId().equals(reply.discussionId()))
                .findFirst()
                .orElseThrow();

        assertNull(parentItem.parentDiscussionId());
        assertEquals("Parent comment", parentItem.comment());
        assertEquals(root.getDiscussionId(), replyItem.parentDiscussionId());
        assertEquals("Child reply", replyItem.comment());
    }

    @Test
    @DisplayName("Soft-deleting a reply hides it from the timeline while parent remains")
    void testSoftDeleteReplyHidesFromTimeline(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount user2 = userAccountManager.findUserAccount(USER2ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent stays", null, null));

        UserDiscussionData reply = discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Reply to delete", null, null));

        problemDiscussionService.softDeleteUserComment(USER2ID, new CommentDeletionRequest(
                user2.getId(),
                problem.getId(),
                reply.discussionId(),
                "Reply to delete",
                USER_DELETED_OWN_DISCUSSION));

        DiscussionRoot deletedReply = discussionRootRepository.findById(reply.discussionId()).orElseThrow();
        assertNotNull(deletedReply.getDeletedAt());
        assertEquals(USER_DELETED_OWN_DISCUSSION, deletedReply.getDeletedReason());

        List<UserDiscussionData> timeline = climbingProblemDiscussionManager.getCommentsForProblem(problem);
        assertTrue(timeline.stream().anyMatch(item -> item.discussionId().equals(root.getDiscussionId())));
        assertTrue(timeline.stream().noneMatch(item -> item.discussionId().equals(reply.discussionId())));
    }

    @Test
    @DisplayName("Replying to a soft-deleted parent fails")
    void testFailReplyToSoftDeletedParent(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Soon deleted", null, null));

        problemDiscussionService.softDeleteUserComment(USER1ID, new CommentDeletionRequest(
                user.getId(),
                problem.getId(),
                root.getDiscussionId(),
                "Soon deleted",
                USER_DELETED_OWN_DISCUSSION));

        assertThrows(RuntimeException.class, () -> discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Should fail", null, null)));
    }

    @Test
    @DisplayName("Reply fails when problem id exists but does not match parent problem")
    void testFailReplyForMismatchedExistingProblem(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problemA = createNewProblem();
        ClimbingProblem problemB = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problemA, user, new DiscussionContentRequest(
                problemA.getId(), "On problem A", null, null));

        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problemB.getId(), user.getId(),
                        "Wrong problem", null, null)));
    }

    @Test
    @DisplayName("Duplicate beta reply to the same parent fails")
    void testFailDuplicateBetaReply(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent", null, null));

        DiscussionContentReplyRequest request = new DiscussionContentReplyRequest(
                root.getDiscussionId(), problem.getId(), user.getId(),
                null, "dup-beta.mp4", "http://dup-beta.com");

        assertNotNull(discussionReplyService.storeUserReply(USER2ID, request));
        assertThrows(IllegalArgumentException.class,
                () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Duplicate mixed reply to the same parent fails")
    void testFailDuplicateMixedReply(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent", null, null));

        DiscussionContentReplyRequest request = new DiscussionContentReplyRequest(
                root.getDiscussionId(), problem.getId(), user.getId(),
                "Same mixed text", "dup-mixed.mp4", "http://dup-mixed.com");

        assertNotNull(discussionReplyService.storeUserReply(USER2ID, request));
        assertThrows(IllegalArgumentException.class,
                () -> discussionReplyService.storeUserReply(USER2ID, request));
    }

    @Test
    @DisplayName("Same comment text on different parents succeeds")
    void testSameCommentOnDifferentParentsSucceeds(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot parent1 = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent one", null, null));
        DiscussionRoot parent2 = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent two", null, null));

        String sameText = "Same reply text";
        UserDiscussionData reply1 = discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        parent1.getDiscussionId(), problem.getId(), user.getId(),
                        sameText, null, null));
        UserDiscussionData reply2 = discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        parent2.getDiscussionId(), problem.getId(), user.getId(),
                        sameText, null, null));

        assertEquals(sameText, reply1.comment());
        assertEquals(sameText, reply2.comment());
        assertEquals(parent1.getDiscussionId(), reply1.parentDiscussionId());
        assertEquals(parent2.getDiscussionId(), reply2.parentDiscussionId());
        assertNotEquals(reply1.discussionId(), reply2.discussionId());
    }

    @Test
    @DisplayName("Unknown firebaseUid fails when creating a reply")
    void testFailReplyForUnknownFirebaseUid(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent", null, null));

        assertThrows(RuntimeException.class, () -> discussionReplyService.storeUserReply(
                "unknownFirebaseUid123",
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Should fail", null, null)));
    }

    @Test
    @DisplayName("Beta-only reply fails when object file or video URL is missing")
    void testFailBetaOnlyReplyWithIncompleteVideoFields(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent", null, null));

        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        null, "only-file.mp4", null)));
        assertThrows(IllegalArgumentException.class, () -> discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        null, null, "http://only-url.com")));
    }

    @Test
    @DisplayName("Another user can report a reply discussion")
    void testReportReplyDiscussion(){
        UserAccount user = userAccountManager.findUserAccount(USER1ID);
        UserAccount admin = userAccountManager.findUserAccount("testFirebaseUid3");
        ClimbingProblem problem = createNewProblem();

        DiscussionRoot root = createNewDiscussion(problem, user, new DiscussionContentRequest(
                problem.getId(), "Parent", null, null));

        UserDiscussionData reply = discussionReplyService.storeUserReply(USER2ID,
                new DiscussionContentReplyRequest(
                        root.getDiscussionId(), problem.getId(), user.getId(),
                        "Reportable reply", null, null));

        assertDoesNotThrow(() -> reportService.createNewReport(
                new ReportRequest(
                        ReportTargetType.DISCUSSION,
                        "This reply is spam",
                        ReportCategoryName.SPAM,
                        reply.discussionId()),
                USER1ID));

        List<Notification> adminNotis = notificationManager.getUserUnreadNotifications(admin);
        assertFalse(adminNotis.isEmpty());
        assertEquals(EventTypeName.REPORT_CREATED,
                adminNotis.get(adminNotis.size() - 1).getEvent().getEventType().getEventTypeName());
    }
}