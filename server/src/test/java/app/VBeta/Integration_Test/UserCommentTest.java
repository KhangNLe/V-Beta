package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.comment.CommentDeletionRequest;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.climb.ClimbingGrade;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.LifecycleStatus;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionComment;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.ClimbingGradeRepository;
import app.VBeta.repository.ClimbingProblemRepository;
import app.VBeta.repository.DiscussionCommentRepository;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.WallSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import app.VBeta.config.TestGcpStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
public class UserCommentTest {
    private static final String USER_DELETED_OWN_DISCUSSION = "User deleted their own discussion";
    private static final String ADMIN_FORCED_DELETE_DISCUSSION = "Admin forced delete the discussion";

    @Autowired
    private ProblemDiscussionService problemDiscussionService;

    @Autowired
    private ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    @Autowired
    private ClimbingProblemRepository climbingProblemRepository;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private DiscussionCommentRepository discussionCommentRepository;

    @Autowired
    private WallSectionRepository wallSectionRepository;
    
    @Autowired
    private ClimbingGradeRepository climbingGradeRepository;

    private boolean checkProblemComments(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        if (account == null) return false;

        List<DiscussionRoot> userCommentRoots = findUserCommentDiscussions(account, request.problemId());
        if (userCommentRoots.isEmpty()) return false;

        return discussionCommentRepository.findByDiscussionRootIn(userCommentRoots).stream()
                .anyMatch(comment -> comment.getCommentInfo().equals(request.commentInfo()));
    }

    @Test
    @DisplayName("Test for a suggest comment added")
    void testForSuccessAddingComment(){
        String firebaseUid1 = "testFirebaseUid";
        DiscussionCommentRequest request1 = new DiscussionCommentRequest(
                2L,
                "Cool problem, the dyno looks sick!"
        );

        String firebaseUid2 = "testFirebaseUid2";
        DiscussionCommentRequest request2 = new DiscussionCommentRequest(
                2L,
                "The back flag on the second move helps a lot"
        );

        String firebaseUid3 = "testFirebaseUid3";
        DiscussionCommentRequest request3 = new DiscussionCommentRequest(
                2L,
                "The last move is very slopey"
        );

        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid1, request1));
        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid2, request2));
        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid3, request3));

        assertTrue(checkProblemComments(firebaseUid1, request1));
        assertTrue(checkProblemComments(firebaseUid2, request2));
        assertTrue(checkProblemComments(firebaseUid3, request3));
    }

    @Test
    @DisplayName("Test for Unknown firebaseUid adding comment")
    void testForUnknownFirebaseUid(){
        String firebaseUid = "123e45";
        DiscussionCommentRequest request = new DiscussionCommentRequest(
                1L,
                "This is a fake comment."
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );
    }

    @Test
    @DisplayName("Test for unexisting problem")
    void testForUnexistingProblem(){
        String firebaseUid = "testFirebaseUid";
        DiscussionCommentRequest request = new DiscussionCommentRequest(
                4L,
                "Cool problem (?)"
        );

        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(account);

        Optional<ClimbingProblem> problem  = climbingProblemRepository.findById(request.problemId());
        assertTrue(problem.isEmpty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );
    }

    @Test
    @DisplayName("Test for adding comment inside archive problem")
    void testForAddingCommentToArchiveProblem(){
        String firebaseUid = "testFirebaseUid";
        DiscussionCommentRequest request = new DiscussionCommentRequest(
                3L,
                "Too bad I can't comment on this"
        );

        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(account);

        Optional<ClimbingProblem> problem = climbingProblemRepository.findById(request.problemId());
        assertTrue(problem.isPresent());
        assertEquals(LifecycleStatus.ARCHIVE, problem.get().getProblemStatus());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );
    }

    @Test
    @DisplayName("test for adding the same comment twice")
    void testForAddingCommentTwice(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;

        assertDoesNotThrow(() -> generateCommentForDeletion(authorUid, problemId));
        assertDoesNotThrow(() -> generateCommentForDeletion(authorUid, problemId));
    }

    @Test
    @DisplayName("test root discussion comment appears in timeline with null parent id")
    void testRootCommentTimelineMapping(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;
        DiscussionCommentRequest request = new DiscussionCommentRequest(problemId, "timeline root comment");

        assertDoesNotThrow(() -> problemDiscussionService.addComment(authorUid, request));

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);

        List<UserDiscussionData> timeline = assertDoesNotThrow(
                () -> climbingProblemDiscussionManager.getCommentsForProblem(problem)
        );

        Optional<UserDiscussionData> created = timeline.stream()
                .filter(item -> item.discussionType() == DiscussionType.COMMENT)
                .filter(item -> request.commentInfo().equals(item.discussionContent()))
                .findFirst();

        assertTrue(created.isPresent());
        assertNull(created.get().parentCommentId());
    }

    @Test
    @DisplayName("test successfully removing comment by a user from a climbing problem: Author")
    void testAuthorRemovingComment(){
        String firebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        CommentDeletionRequest deletionRequest = generateCommentForDeletion(
                firebaseUid, problemId, USER_DELETED_OWN_DISCUSSION);

        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        assertDoesNotThrow(() -> problemDiscussionService.softDeleteUserComment(firebaseUid, deletionRequest));
        assertEquals(0, countVisibleCommentsForUserProblem(userAccount, problem.getId()));
        assertDiscussionSoftDeleted(deletionRequest.discussionId(), userAccount, USER_DELETED_OWN_DISCUSSION);
        assertCommentRowStillPresent(deletionRequest.discussionId());
        assertFalse(timelineContainsDiscussion(problem, deletionRequest.discussionId()));
    }

    @Test
    @DisplayName("test successfully removing comment by a user from climbing problem: Admin")
    void testAdminRemovingComment(){
        String authorFirebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        String adminFirebaseUid = "testFirebaseUid3";

        CommentDeletionRequest deletionRequest = generateCommentForDeletion(
                authorFirebaseUid, problemId, ADMIN_FORCED_DELETE_DISCUSSION);

        UserAccount authorUserAccount = userAccountManager.findUserAccount(authorFirebaseUid);
        UserAccount adminAccount = userAccountManager.findUserAccount(adminFirebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        assertDoesNotThrow(() -> problemDiscussionService.softDeleteUserComment(adminFirebaseUid, deletionRequest));
        assertEquals(0, countVisibleCommentsForUserProblem(authorUserAccount, problem.getId()));
        assertDiscussionSoftDeleted(deletionRequest.discussionId(), adminAccount, ADMIN_FORCED_DELETE_DISCUSSION);
        assertCommentRowStillPresent(deletionRequest.discussionId());
        assertFalse(timelineContainsDiscussion(problem, deletionRequest.discussionId()));
    }

    @Test
    @DisplayName("test failure removing comment by a user from climbing problem: Another User")
    void testFailRemovingComment(){
        String authorFirebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        String requesterFirebaseUid = "testFirebaseUid2";

        CommentDeletionRequest deletionRequest = generateCommentForDeletion(
                authorFirebaseUid, problemId, USER_DELETED_OWN_DISCUSSION);

        UserAccount authorUserAccount = userAccountManager.findUserAccount(authorFirebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemDiscussionService.softDeleteUserComment(requesterFirebaseUid, deletionRequest));

        assertEquals(1, countVisibleCommentsForUserProblem(authorUserAccount, problem.getId()));
    }

    @Test
    @DisplayName("test failure for requesting an unexisting comment")
    void testFailureDeletingUnexistingComment(){
        String requestFirebaseUid = "testFirebaseUid";
        UserAccount userAccount = userAccountManager.findUserAccount(requestFirebaseUid);
        Long problemId = 2L;

        CommentDeletionRequest request = new CommentDeletionRequest(
                userAccount.getId(),
                problemId,
                999999L,
                "blahblahblahblah",
                USER_DELETED_OWN_DISCUSSION
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemDiscussionService.softDeleteUserComment(requestFirebaseUid, request));
    }

    @Test
    @DisplayName("test failure for request deletion from an existing comment by changing authorId")
    void testFailureDeletingExistingCommentByChangingId(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;
        UserAccount author = userAccountManager.findUserAccount(authorUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        CommentDeletionRequest realRequest = generateCommentForDeletion(
                authorUid, problemId, USER_DELETED_OWN_DISCUSSION);

        String threatActorUid = "testFirebaseUid2";
        UserAccount threatActor = userAccountManager.findUserAccount(threatActorUid);
        assertNotNull(threatActor);

        CommentDeletionRequest modifiedRequest = new CommentDeletionRequest(
                threatActor.getId(),
                realRequest.problemId(),
                realRequest.discussionId(),
                realRequest.commentContent(),
                USER_DELETED_OWN_DISCUSSION
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemDiscussionService.softDeleteUserComment(threatActorUid, modifiedRequest));

        assertEquals(1, countVisibleCommentsForUserProblem(author, problem.getId()));
    }

    @Test
    @DisplayName("test cross-problem isolation when deleting comment")
    void testCrossProblemIsolationDeletingComment(){
        String firebaseUid = "testFirebaseUid";
        String sameComment = "same text across two problems";
        Long problemIdA = createNewClimbingProblem();
        Long problemIdB = 2L;

        DiscussionCommentRequest requestA = new DiscussionCommentRequest(problemIdA, sameComment);
        DiscussionCommentRequest requestB = new DiscussionCommentRequest(problemIdB, sameComment);

        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid, requestA));
        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid, requestB));
        assertTrue(checkProblemComments(firebaseUid, requestA));
        assertTrue(checkProblemComments(firebaseUid, requestB));

        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(userAccount);

        CommentDeletionRequest deleteFromProblemA = new CommentDeletionRequest(
                userAccount.getId(),
                problemIdA,
                findLatestCommentDiscussionId(userAccount, problemIdA),
                sameComment,
                USER_DELETED_OWN_DISCUSSION
        );

        assertDoesNotThrow(() -> problemDiscussionService.softDeleteUserComment(firebaseUid, deleteFromProblemA));
        assertDiscussionSoftDeleted(deleteFromProblemA.discussionId(), userAccount, USER_DELETED_OWN_DISCUSSION);
        assertCommentRowStillPresent(deleteFromProblemA.discussionId());

        ClimbingProblem problemA = climbingProblemManager.getActiveProblem(problemIdA);
        ClimbingProblem problemB = climbingProblemManager.getActiveProblem(problemIdB);
        assertNotNull(problemA);
        assertNotNull(problemB);

        assertEquals(0, countVisibleCommentsForUserProblem(userAccount, problemA.getId()));
        assertEquals(1, countVisibleCommentsForUserProblem(userAccount, problemB.getId()));
    }

    @Test
    @DisplayName("test failure deleting same comment twice")
    void testDeletingSameCommentTwice(){
        String firebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        CommentDeletionRequest deletionRequest = generateCommentForDeletion(
                firebaseUid, problemId, USER_DELETED_OWN_DISCUSSION);

        assertDoesNotThrow(() -> problemDiscussionService.softDeleteUserComment(firebaseUid, deletionRequest));
        assertDiscussionSoftDeleted(
                deletionRequest.discussionId(),
                userAccountManager.findUserAccount(firebaseUid),
                USER_DELETED_OWN_DISCUSSION);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemDiscussionService.softDeleteUserComment(firebaseUid, deletionRequest));
    }

    @Test
    @DisplayName("test failure admin deleting comment with mismatched payload")
    void testAdminDeletingCommentWithMismatchedPayload(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;
        CommentDeletionRequest realRequest = generateCommentForDeletion(
                authorUid, problemId, ADMIN_FORCED_DELETE_DISCUSSION);

        String adminUid = "testFirebaseUid3";
        UserAccount author = userAccountManager.findUserAccount(authorUid);
        assertNotNull(author);

        CommentDeletionRequest mismatchedRequest = new CommentDeletionRequest(
                author.getId(),
                realRequest.problemId(),
                realRequest.discussionId(),
                realRequest.commentContent() + " - modified",
                ADMIN_FORCED_DELETE_DISCUSSION
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemDiscussionService.softDeleteUserComment(adminUid, mismatchedRequest));

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertEquals(1, countVisibleCommentsForUserProblem(author, problem.getId()));
    }

    @Test
    @DisplayName("test for user adding comment twice and delete one of them")
    void testRemoveOneOfDuplicateComment(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;

        UserAccount author = userAccountManager.findUserAccount(authorUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        CommentDeletionRequest requestA = generateCommentForDeletion(
                authorUid, problemId, USER_DELETED_OWN_DISCUSSION);
        CommentDeletionRequest requestB = generateCommentForDeletion(
                authorUid, problemId, USER_DELETED_OWN_DISCUSSION);

        assertEquals(2, countVisibleCommentsForUserProblem(author, problem.getId()));

        assertDoesNotThrow(() -> problemDiscussionService.softDeleteUserComment(authorUid, requestA));

        assertEquals(1, countVisibleCommentsForUserProblem(author, problem.getId()));
        assertDiscussionSoftDeleted(requestA.discussionId(), author, USER_DELETED_OWN_DISCUSSION);
        assertCommentRowStillPresent(requestA.discussionId());
        assertCommentRowStillPresent(requestB.discussionId());
        assertFalse(timelineContainsDiscussion(problem, requestA.discussionId()));
        assertTrue(timelineContainsDiscussion(problem, requestB.discussionId()));
    }

    private CommentDeletionRequest generateCommentForDeletion(String firebaseUid, Long problemId){
        return generateCommentForDeletion(firebaseUid, problemId, USER_DELETED_OWN_DISCUSSION);
    }

    private CommentDeletionRequest generateCommentForDeletion(String firebaseUid, Long problemId,
                                                              String deletedReason){
        DiscussionCommentRequest addingRequest = new DiscussionCommentRequest(
                problemId,
                "Cool problem dude"
        );

        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        //Check to make sure there a comment successfully add from a user
        assertNotNull(userAccount);
        assertNotNull(problem);
        assertDoesNotThrow(() -> problemDiscussionService.addComment(firebaseUid, addingRequest));
        assertTrue(checkProblemComments(firebaseUid, addingRequest));
        List<DiscussionRoot> userCommentRoots = findUserCommentDiscussions(userAccount, problemId);
        assertFalse(userCommentRoots.isEmpty());

        List<DiscussionComment> comments = discussionCommentRepository.findByDiscussionRootIn(userCommentRoots);
        Optional<DiscussionComment> comment = comments.stream()
                .filter(c -> c.getCommentInfo().equals(addingRequest.commentInfo()))
                .filter(c -> c.getDiscussionRoot().getDeletedAt() == null)
                .max(Comparator.comparing(c -> c.getDiscussionRoot().getDiscussionId()));

        assertTrue(comment.isPresent());

        return new CommentDeletionRequest(
                userAccount.getId(),
                problemId,
                comment.get().getDiscussionRoot().getDiscussionId(),
                addingRequest.commentInfo(),
                deletedReason
        );
    }

    // Creating new climbing problem has yet been implement, therefore, this will do for now
    private Long createNewClimbingProblem(){
        Optional<WallSection> wallSection = wallSectionRepository.findById(1L);
        assertTrue(wallSection.isPresent());

        List<ClimbingGrade> grades = climbingGradeRepository.findAll();
        assertFalse(grades.isEmpty());

        ClimbingProblem problem = new ClimbingProblem();
        problem.setCreatedDate(LocalDateTime.now());
        problem.setProblemStatus(LifecycleStatus.ACTIVE);
        problem.setProblemInfo("test problem");
        problem.setWallSection(wallSection.get());
        problem.setHoldColor("Red");
        problem.setClimbingGrade(grades.get(0));
        problem = climbingProblemRepository.save(problem);

        return problem.getId();
    }

    private List<DiscussionRoot> findUserCommentDiscussions(UserAccount userAccount, Long problemId) {
        return discussionRootRepository.findByUserAccount_AndDiscussionType(userAccount, DiscussionType.COMMENT)
                .stream()
                .filter(root -> root.getProblem().getId().equals(problemId))
                .toList();
    }

    private int countVisibleCommentsForUserProblem(UserAccount userAccount, Long problemId) {
        List<DiscussionRoot> userCommentRoots = findUserCommentDiscussions(userAccount, problemId).stream()
                .filter(root -> root.getDeletedAt() == null)
                .toList();
        if (userCommentRoots.isEmpty()) {
            return 0;
        }
        return discussionCommentRepository.findByDiscussionRootIn(userCommentRoots).size();
    }

    private void assertDiscussionSoftDeleted(Long discussionId, UserAccount actor, String expectedReason) {
        DiscussionRoot deleted = discussionRootRepository.findById(discussionId).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertEquals(expectedReason, deleted.getDeletedReason());
        assertNotNull(deleted.getDeletedBy());
        assertEquals(actor.getId(), deleted.getDeletedBy().getId());
    }

    private void assertCommentRowStillPresent(Long discussionId) {
        DiscussionRoot root = discussionRootRepository.findById(discussionId).orElseThrow();
        assertTrue(discussionCommentRepository.findByDiscussionRoot(root).isPresent());
    }

    private boolean timelineContainsDiscussion(ClimbingProblem problem, Long discussionId) {
        return climbingProblemDiscussionManager.getCommentsForProblem(problem).stream()
                .anyMatch(item -> item.discussionId().equals(discussionId));
    }

    private Long findLatestCommentDiscussionId(UserAccount userAccount, Long problemId) {
        return findUserCommentDiscussions(userAccount, problemId).stream()
                .map(DiscussionRoot::getDiscussionId)
                .max(Long::compareTo)
                .orElseThrow();
    }
}
