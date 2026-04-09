package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.UserCommentData;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.support.ClimbingProblemDiscussionManager;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3307}/${MYSQL_DB:V_Beta}",
        "spring.datasource.username=${MYSQL_USERNAME:${SQL_USERNAME:khang}}",
        "spring.datasource.password=${MYSQL_PASSWORD:${SQL_PASSWORD:}}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
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

    private boolean checkProblemComments(String firebaseUid, DiscussionCommentRequest request){
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(request.problemId());
        List<UserCommentData> comments = climbingProblemDiscussionManager.getCommentsForProblem(problem);

        if (account == null || problem == null || comments.isEmpty()) return false;

        return comments.stream().filter(data ->
            data.userId().equals(account.getId()) && data.comment().equals(request.commentInfo())
            )
            .count() == 1;
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
}
