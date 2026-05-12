package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.DiscussionRootManager;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.DiscussionRootRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
                "spring.datasource.username=${SQL_USERNAME:postgres}",
                "spring.datasource.password=${SQL_PASSWORD:postgres}",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        }
)
public class DiscussionRootTest {
    @Autowired
    private DiscussionRootManager discussionRootManager;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    private UserAccount getTestUser(String firebaseUid) {
        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        assertNotNull(userAccount);
        return userAccount;
    }

    private ClimbingProblem getTestClimbingProblem(long problemId) {
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        return problem;
    }

    @Test
    @DisplayName("Test success discussion root creation with no parent")
    void testDiscussionRootCreation() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);

        DiscussionRoot discussionRoot = discussionRootManager.createNewDiscussion(testUser, problem,
                DiscussionType.COMMENT);

        assertNotNull(discussionRoot);
        assertEquals(testUser, discussionRoot.getUserAccount());
        assertEquals(problem, discussionRoot.getProblem());
        assertEquals(DiscussionType.COMMENT, discussionRoot.getDiscussionType());
        assertNull(discussionRoot.getParent());
        assertNotNull(discussionRoot.getCreatedAt());
        assertNull(discussionRoot.getDeletedAt());
        assertNull(discussionRoot.getDeletedReason());
        assertNull(discussionRoot.getDeletedBy());
    }

    @Test
    @DisplayName("Test success creation for child discussion thread")
    void testSubDiscussionThreadCreation() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);
        DiscussionRoot parent = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.COMMENT);

        DiscussionRoot child = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.BETA, parent
        );

        assertNotNull(child);
        assertNotNull(child.getDiscussionId());
        assertNotNull(child.getParent());
        assertEquals(parent.getDiscussionId(), child.getParent().getDiscussionId());
    }

    @Test
    @DisplayName("Test for finding only head discussion threads by problem")
    void testFindHeadDiscussionThreadsByProblem() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);

        DiscussionRoot rootA = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.COMMENT);
        DiscussionRoot rootB = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.BETA);
        DiscussionRoot reply = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.COMMENT, rootA
        );

        List<DiscussionRoot> roots = discussionRootRepository.findByProblem_IdAndParentIsNullOrderByCreatedAtDesc(problemId);

        assertTrue(roots.stream().anyMatch(r -> r.getDiscussionId().equals(rootA.getDiscussionId())));
        assertTrue(roots.stream().anyMatch(r -> r.getDiscussionId().equals(rootB.getDiscussionId())));
        assertFalse(roots.stream().anyMatch(r -> r.getDiscussionId().equals(reply.getDiscussionId())));
        assertTrue(roots.stream().allMatch(r -> r.getParent() == null));
    }

    @Test
    @DisplayName("Test for finding replies by parent discussion id")
    void testFindActiveRepliesByParentId() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);

        DiscussionRoot parent = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.COMMENT);
        DiscussionRoot activeReply = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.COMMENT, parent
        );
        DiscussionRoot secondReply = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.BETA, parent
        );

        List<DiscussionRoot> replies = discussionRootRepository
                .findByParent_DiscussionIdOrderByCreatedAtDesc(parent.getDiscussionId());

        assertTrue(replies.stream().anyMatch(r -> r.getDiscussionId().equals(activeReply.getDiscussionId())));
        assertTrue(replies.stream().anyMatch(r -> r.getDiscussionId().equals(secondReply.getDiscussionId())));
    }

    @Test
    @DisplayName("Test enum discussion type persists for comment and beta")
    void testDiscussionTypeEnumPersisted() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);

        DiscussionRoot commentRoot = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.COMMENT);
        DiscussionRoot betaRoot = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.BETA);

        DiscussionRoot loadedComment = discussionRootRepository.findById(commentRoot.getDiscussionId()).orElseThrow();
        DiscussionRoot loadedBeta = discussionRootRepository.findById(betaRoot.getDiscussionId()).orElseThrow();

        assertEquals(DiscussionType.COMMENT, loadedComment.getDiscussionType());
        assertEquals(DiscussionType.BETA, loadedBeta.getDiscussionType());
    }

    @Test
    @DisplayName("Test invalid parent link is blocked by referential integrity")
    void testInvalidParentReferenceRejected() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);

        DiscussionRoot invalidDiscussion = new DiscussionRoot();
        invalidDiscussion.setUserAccount(testUser);
        invalidDiscussion.setProblem(problem);
        invalidDiscussion.setDiscussionType(DiscussionType.COMMENT);
        invalidDiscussion.setParent(createMissingParentReference());

        assertThrows(DataIntegrityViolationException.class, () -> discussionRootRepository.saveAndFlush(invalidDiscussion));
    }

    @Test
    @DisplayName("Test reply query returns newest first")
    void testFindRepliesByParentIdOrderByCreatedAtDesc() {
        String fakeFirebaseID = "testFirebaseUid";
        long problemId = 1L;

        UserAccount testUser = getTestUser(fakeFirebaseID);
        ClimbingProblem problem = getTestClimbingProblem(problemId);
        DiscussionRoot parent = discussionRootManager.createNewDiscussion(testUser, problem, DiscussionType.COMMENT);

        DiscussionRoot oldest = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.COMMENT, parent
        );
        DiscussionRoot newest = discussionRootManager.createSubDiscussionThread(
                testUser, problem, DiscussionType.BETA, parent
        );

        oldest.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        newest.setCreatedAt(LocalDateTime.now());
        discussionRootRepository.saveAndFlush(oldest);
        discussionRootRepository.saveAndFlush(newest);

        List<DiscussionRoot> replies = discussionRootRepository
                .findByParent_DiscussionIdOrderByCreatedAtDesc(parent.getDiscussionId());

        assertFalse(replies.isEmpty());
        assertEquals(newest.getDiscussionId(), replies.get(0).getDiscussionId());
        assertEquals(oldest.getDiscussionId(), replies.get(replies.size() - 1).getDiscussionId());
    }

    @Test
    @DisplayName("Test finding discussions by user and problem")
    void testFindDiscussionsByUserAndProblem() {
        UserAccount userOne = getTestUser("testFirebaseUid");
        UserAccount userTwo = getTestUser("testFirebaseUid2");
        ClimbingProblem problemOne = getTestClimbingProblem(1L);
        ClimbingProblem problemTwo = getTestClimbingProblem(2L);

        DiscussionRoot matchA = discussionRootManager.createNewDiscussion(userOne, problemOne, DiscussionType.COMMENT);
        DiscussionRoot matchB = discussionRootManager.createNewDiscussion(userOne, problemOne, DiscussionType.BETA);
        discussionRootManager.createNewDiscussion(userOne, problemTwo, DiscussionType.COMMENT);
        discussionRootManager.createNewDiscussion(userTwo, problemOne, DiscussionType.COMMENT);

        List<DiscussionRoot> matches = discussionRootRepository.findByUserAccount_AndProblem(userOne, problemOne);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().anyMatch(d -> d.getDiscussionId().equals(matchA.getDiscussionId())));
        assertTrue(matches.stream().anyMatch(d -> d.getDiscussionId().equals(matchB.getDiscussionId())));
        assertTrue(matches.stream().allMatch(d ->
                d.getUserAccount().getId().equals(userOne.getId()) &&
                        d.getProblem().getId().equals(problemOne.getId())
        ));
    }

    private DiscussionRoot createMissingParentReference() {
        DiscussionRoot missingParent = new DiscussionRoot();
        missingParent.setDiscussionId(999_999L);
        return missingParent;
    }
}