package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.CommentDeletionRequest;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.application.support.WallSectionManager;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import edu.ics499.VBeta.repository.DiscussionRootRepository;
import edu.ics499.VBeta.repository.WallSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.ics499.VBeta.config.TestGcpStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotEquals(HttpStatus.OK, ex.getStatusCode());
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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotEquals(HttpStatus.OK, ex.getStatusCode());
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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                problemDiscussionService.addComment(firebaseUid, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotEquals(HttpStatus.OK, ex.getStatusCode());
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

        List<UserCommentData> timeline = assertDoesNotThrow(
                () -> climbingProblemDiscussionManager.getCommentsForProblem(problem)
        );

        Optional<UserCommentData> created = timeline.stream()
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
        CommentDeletionRequest deletionRequest = generateCommentForDeletion(firebaseUid, problemId);

        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        // Check for deleting comment from the user
        assertDoesNotThrow(() -> problemDiscussionService.removeUserComment(firebaseUid, deletionRequest));
        assertEquals(0, countVisibleCommentsForUserProblem(userAccount, problem.getId()));
    }

    @Test
    @DisplayName("test successfully removing comment by a user from climbing problem: Admin")
    void testAdminRemovingComment(){
        String authorFirebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        String adminFirebaseUid = "testFirebaseUid3";

        CommentDeletionRequest deletionRequest = generateCommentForDeletion(authorFirebaseUid, problemId);

        UserAccount authorUserAccount = userAccountManager.findUserAccount(authorFirebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        assertDoesNotThrow(() -> problemDiscussionService.removeUserComment(adminFirebaseUid, deletionRequest));
        assertEquals(0, countVisibleCommentsForUserProblem(authorUserAccount, problem.getId()));
    }

    @Test
    @DisplayName("test failure removing comment by a user from climbing problem: Another User")
    void testFailRemovingComment(){
        String authorFirebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        String requesterFirebaseUid = "testFirebaseUid2";

        CommentDeletionRequest deletionRequest = generateCommentForDeletion(authorFirebaseUid, problemId);

        UserAccount authorUserAccount = userAccountManager.findUserAccount(authorFirebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> problemDiscussionService.removeUserComment(requesterFirebaseUid, deletionRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

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
                "blahblahblahblah"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> problemDiscussionService.removeUserComment(requestFirebaseUid, request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("test failure for request deletion from an existing comment by changing authorId")
    void testFailureDeletingExistingCommentByChangingId(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;
        UserAccount author = userAccountManager.findUserAccount(authorUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);

        CommentDeletionRequest realRequest = generateCommentForDeletion(authorUid, problemId);

        String threatActorUid = "testFirebaseUid2";
        UserAccount threatActor = userAccountManager.findUserAccount(threatActorUid);
        assertNotNull(threatActor);

        CommentDeletionRequest modifiedRequest = new CommentDeletionRequest(
                threatActor.getId(),
                realRequest.problemId(),
                realRequest.discussionId(),
                realRequest.commentContent()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> problemDiscussionService.removeUserComment(threatActorUid, modifiedRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

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
                sameComment
        );

        assertDoesNotThrow(() -> problemDiscussionService.removeUserComment(firebaseUid, deleteFromProblemA));

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
        CommentDeletionRequest deletionRequest = generateCommentForDeletion(firebaseUid, problemId);

        assertDoesNotThrow(() -> problemDiscussionService.removeUserComment(firebaseUid, deletionRequest));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> problemDiscussionService.removeUserComment(firebaseUid, deletionRequest));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("test failure admin deleting comment with mismatched payload")
    void testAdminDeletingCommentWithMismatchedPayload(){
        String authorUid = "testFirebaseUid";
        Long problemId = 2L;
        CommentDeletionRequest realRequest = generateCommentForDeletion(authorUid, problemId);

        String adminUid = "testFirebaseUid3";
        UserAccount author = userAccountManager.findUserAccount(authorUid);
        assertNotNull(author);

        CommentDeletionRequest mismatchedRequest = new CommentDeletionRequest(
                author.getId(),
                realRequest.problemId(),
                realRequest.discussionId(),
                realRequest.commentContent() + " - modified"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> problemDiscussionService.removeUserComment(adminUid, mismatchedRequest));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

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

        CommentDeletionRequest requestA = generateCommentForDeletion(authorUid, problemId);
        CommentDeletionRequest requestB = generateCommentForDeletion(authorUid, problemId);

        assertEquals(2, countVisibleCommentsForUserProblem(author, problem.getId()));

        assertDoesNotThrow(() -> problemDiscussionService.removeUserComment(authorUid, requestA));

        assertEquals(1, countVisibleCommentsForUserProblem(author, problem.getId()));
    }

    private CommentDeletionRequest generateCommentForDeletion(String firebaseUid, Long problemId){
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
                .findFirst();

        assertTrue(comment.isPresent());

        return new CommentDeletionRequest(
                userAccount.getId(),
                problemId,
                comment.get().getDiscussionRoot().getDiscussionId(),
                addingRequest.commentInfo()
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
        List<DiscussionRoot> userCommentRoots = findUserCommentDiscussions(userAccount, problemId);
        return discussionCommentRepository.findByDiscussionRootIn(userCommentRoots).size();
    }

    private Long findLatestCommentDiscussionId(UserAccount userAccount, Long problemId) {
        return findUserCommentDiscussions(userAccount, problemId).stream()
                .map(DiscussionRoot::getDiscussionId)
                .max(Long::compareTo)
                .orElseThrow();
    }
}
