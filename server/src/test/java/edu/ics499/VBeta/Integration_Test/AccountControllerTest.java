package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.api.dto.PerceiveGradeRequest;
import edu.ics499.VBeta.api.dto.SolutionBetaCreateRequest;
import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.application.support.GcpFileStorageAdapter;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.GradeDefinition;
import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.SolutionBeta;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.domain.model.UserBeta;
import edu.ics499.VBeta.domain.model.UserComment;
import edu.ics499.VBeta.repository.DiscussionCommentRepository;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.SolutionBetaRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
import edu.ics499.VBeta.repository.UserBetaRepository;
import edu.ics499.VBeta.repository.UserCommentRepository;
import edu.ics499.VBeta.repository.UserPerceiveGradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
"spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
        "spring.datasource.username=${SQL_USERNAME:khang}",
        "spring.datasource.password=${SQL_PASSWORD:}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
public class AccountControllerTest {
    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private GymRoleRepository gymRoleRepository;
    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private ProblemDiscussionService problemDiscussionService;

    @Autowired
    private UserCommentRepository userCommentRepository;

    @Autowired
    private DiscussionCommentRepository discussionCommentRepository;

    @Autowired
    private UserPerceiveGradeRepository userPerceiveGradeRepository;

    @Autowired
    private UserBetaRepository userBetaRepository;

    @Autowired
    private SolutionBetaRepository solutionBetaRepository;

    @MockitoBean
    private GcpFileStorageAdapter gcpFileStorageAdapter;


    @Test
    void testAccountConnection() throws Exception {
        // Ensure prerequisite role exists for account creation path.
        if (gymRoleRepository.findByRoleType(RoleType.CLIMBER).isEmpty()) {
            GymRole gymRole = new GymRole();
            gymRole.setRoleType(RoleType.CLIMBER);
            gymRoleRepository.save(gymRole);
        }

        AccountRequest req = new AccountRequest(
            "testUser",
            "testUser@gmail.com"
        );
        String testFirebaseUid = "testFirebaseUid";

        AccountResponse resp = accountService.loginAccount(req.username(), req.email(), testFirebaseUid);
        assertTrue(accountRepository.findByFirebaseUid(testFirebaseUid).isPresent());
        assertNotNull(resp);
        assertNotNull(resp.id());
        assertEquals(testFirebaseUid, resp.firebaseUid());
        assertEquals("testUser", resp.username());
        assertEquals("testUser@gmail.com", resp.email());
    }

    @Test
    @DisplayName("test creating new account")
    void testCreatingNewAccount(){
        String userName = "test userName";
        String email = "fakeEmail123@gmail.com";
        String firebaseUid = "testFakeFirebaseUid";
        assertNotNull(creatFakeAccount(userName, email, firebaseUid));
    }

    @Test
    @DisplayName("test delete user account")
    void testDeleteUserAccount(){
        String userName = "fakeName";
        String email = "fakeEmail";
        String firebaseUid = "FAKEID";

        UserAccount account = creatFakeAccount(userName, email, firebaseUid);
        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));

        account = userAccountManager.findUserAccount(firebaseUid);
        assertNull(account);
    }

    @Test
    @DisplayName("test delete user with an unexisting firebaseUid")
    void testFailureAccountDeletion(){
        String fakeFirebaseUid = "FAKEID";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountService.deleteAccount(fakeFirebaseUid));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("test account deletion clears comments, betas, perceive grades, and account row")
    void testDeleteAccountRemovesAllRelatedData() {
        String userName = "cleanup-user";
        String email = "cleanup-user@gmail.com";
        String firebaseUid = "cleanup-user-firebase-uid";
        Long problemId = 2L;

        UserAccount account = creatFakeAccount(userName, email, firebaseUid);
        when(gcpFileStorageAdapter.getPublicBucketName()).thenReturn("test-bucket");

        problemDiscussionService.addComment(
                firebaseUid,
                new DiscussionCommentRequest(problemId, "Delete cascade test comment")
        );
        problemDiscussionService.addClimbingProblemPerceiveGrade(
                firebaseUid,
                problemId,
                new PerceiveGradeRequest(GradeDefinition.V7)
        );

        String objectKey = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrl = "https://storage.googleapis.com/test-bucket/" + objectKey;
        problemDiscussionService.saveSolutionBeta(
                new SolutionBetaCreateRequest(problemId, objectKey, publicUrl),
                firebaseUid
        );

        List<UserComment> userComments = userCommentRepository.findByUserAccount(account);
        assertFalse(userComments.isEmpty());
        assertFalse(discussionCommentRepository.findByUserCommentIn(userComments).isEmpty());
        assertFalse(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        List<UserBeta> userBetas = userBetaRepository.findByUser(account);
        assertFalse(userBetas.isEmpty());
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByUserBetaIn(userBetas);
        assertFalse(solutionBetas.isEmpty());

        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKey));

        assertNull(userAccountManager.findUserAccount(firebaseUid));
        assertTrue(userCommentRepository.findByUserAccount(account).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(userBetaRepository.findByUser(account).isEmpty());
        assertTrue(accountRepository.findById(account.getId()).isEmpty());
    }

    @Test
    @DisplayName("test deleting account with no related data still succeeds")
    void testDeleteAccountWithNoRelatedData() {
        String firebaseUid = "no-related-data-user";
        UserAccount account = creatFakeAccount("no-data-user", "no-data@gmail.com", firebaseUid);

        assertTrue(userCommentRepository.findByUserAccount(account).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(userBetaRepository.findByUser(account).isEmpty());

        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));
        verify(gcpFileStorageAdapter, never()).deleteFile(anyString(), anyString());

        assertNull(userAccountManager.findUserAccount(firebaseUid));
        assertTrue(accountRepository.findById(account.getId()).isEmpty());
    }

    @Test
    @DisplayName("test account deletion removes multiple comments and multiple betas")
    void testDeleteAccountWithMultipleRelatedRows() {
        String firebaseUid = "multi-related-user-firebase-uid";
        Long problemId = 2L;
        UserAccount account = creatFakeAccount("multi-related-user", "multi-related-user@gmail.com", firebaseUid);
        when(gcpFileStorageAdapter.getPublicBucketName()).thenReturn("test-bucket");

        problemDiscussionService.addComment(firebaseUid, new DiscussionCommentRequest(problemId, "comment one"));
        problemDiscussionService.addComment(firebaseUid, new DiscussionCommentRequest(problemId, "comment two"));
        problemDiscussionService.addClimbingProblemPerceiveGrade(
                firebaseUid,
                problemId,
                new PerceiveGradeRequest(GradeDefinition.V10)
        );

        String objectKeyOne = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrlOne = "https://storage.googleapis.com/test-bucket/" + objectKeyOne;
        problemDiscussionService.saveSolutionBeta(
                new SolutionBetaCreateRequest(problemId, objectKeyOne, publicUrlOne),
                firebaseUid
        );

        String objectKeyTwo = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrlTwo = "https://storage.googleapis.com/test-bucket/" + objectKeyTwo;
        problemDiscussionService.saveSolutionBeta(
                new SolutionBetaCreateRequest(problemId, objectKeyTwo, publicUrlTwo),
                firebaseUid
        );

        List<UserComment> userComments = userCommentRepository.findByUserAccount(account);
        assertEquals(2, userComments.size());
        assertEquals(2, discussionCommentRepository.findByUserCommentIn(userComments).size());
        assertEquals(1, userPerceiveGradeRepository.findByUserAccount(account).size());
        List<UserBeta> userBetas = userBetaRepository.findByUser(account);
        assertEquals(2, userBetas.size());
        assertEquals(2, solutionBetaRepository.findByUserBetaIn(userBetas).size());

        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKeyOne));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKeyTwo));

        assertNull(userAccountManager.findUserAccount(firebaseUid));
        assertTrue(userCommentRepository.findByUserAccount(account).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(userBetaRepository.findByUser(account).isEmpty());
        assertTrue(accountRepository.findById(account.getId()).isEmpty());
    }

    @Test
    @DisplayName("test account deletion fails when a discussion comment row is missing")
    void testDeleteAccountFailsForMissingDiscussionComment() {
        String firebaseUid = "missing-discussion-comment-user";
        Long problemId = 2L;
        UserAccount account = creatFakeAccount("missing-discussion", "missing-discussion@gmail.com", firebaseUid);

        problemDiscussionService.addComment(
                firebaseUid,
                new DiscussionCommentRequest(problemId, "comment that will lose its discussion row")
        );

        List<UserComment> userComments = userCommentRepository.findByUserAccount(account);
        assertEquals(1, userComments.size());
        UserComment commentAnchor = userComments.get(0);
        assertTrue(discussionCommentRepository.findByUserComment(commentAnchor).isPresent());
        discussionCommentRepository.delete(discussionCommentRepository.findByUserComment(commentAnchor).get());
        assertTrue(discussionCommentRepository.findByUserComment(commentAnchor).isEmpty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.deleteAccount(firebaseUid)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertNotNull(userAccountManager.findUserAccount(firebaseUid));
    }

    @Test
    @DisplayName("test account deletion fails when a solution beta row is missing")
    void testDeleteAccountFailsForMissingSolutionBeta() {
        String firebaseUid = "missing-solution-beta-user";
        Long problemId = 2L;
        UserAccount account = creatFakeAccount("missing-solution", "missing-solution@gmail.com", firebaseUid);

        String objectKey = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrl = "https://storage.googleapis.com/test-bucket/" + objectKey;
        problemDiscussionService.saveSolutionBeta(
                new SolutionBetaCreateRequest(problemId, objectKey, publicUrl),
                firebaseUid
        );

        List<UserBeta> userBetas = userBetaRepository.findByUser(account);
        assertEquals(1, userBetas.size());
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByUserBetaIn(userBetas);
        assertEquals(1, solutionBetas.size());
        solutionBetaRepository.delete(solutionBetas.get(0));
        assertTrue(solutionBetaRepository.findByUserBetaIn(userBetas).isEmpty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> accountService.deleteAccount(firebaseUid)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertNotNull(userAccountManager.findUserAccount(firebaseUid));
    }

    private UserAccount creatFakeAccount(String userName, String email, String firebaseUid){
        assertDoesNotThrow(() -> userAccountManager.createNewAccount(userName, email, firebaseUid));
        UserAccount account = userAccountManager.findUserAccountWithRole(firebaseUid);
        assertNotNull(account);
        assertEquals(userName, account.getUsername());
        assertEquals(email, account.getEmail());
        assertEquals(firebaseUid, account.getFirebaseUid());
        assertEquals(RoleType.CLIMBER, account.getGymRole().getRoleType());

        return account;
    }
}