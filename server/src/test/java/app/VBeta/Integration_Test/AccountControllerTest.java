package app.VBeta.Integration_Test;

import app.VBeta.api.dto.account.AccountRequest;
import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.api.dto.discussions.PerceiveGradeRequest;
import app.VBeta.api.dto.discussions.video.SolutionBetaCreateRequest;
import app.VBeta.application.AccountService;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.application.support.discussion.beta.GcpFileStorageAdapter;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.DiscussionCommentRepository;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.GymRoleRepository;
import app.VBeta.repository.SolutionBetaRepository;
import app.VBeta.repository.UserAccountRepository;
import app.VBeta.repository.UserPerceiveGradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import app.VBeta.config.TestGcpStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private DiscussionCommentRepository discussionCommentRepository;

    @Autowired
    private UserPerceiveGradeRepository userPerceiveGradeRepository;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

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

        UserAccountDTO resp = accountService.loginAccount(req.username(), req.email(), testFirebaseUid);
        assertTrue(accountRepository.findByFirebaseUid(testFirebaseUid).isPresent());
        assertNotNull(resp);
        assertNotNull(resp.userId());
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

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.deleteAccount(fakeFirebaseUid));
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

        List<DiscussionRoot> commentRoots = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.COMMENT
        );
        assertFalse(commentRoots.isEmpty());
        assertFalse(discussionCommentRepository.findByDiscussionRootIn(commentRoots).isEmpty());
        assertFalse(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        List<DiscussionRoot> userBetas = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.BETA
        );
        assertFalse(userBetas.isEmpty());
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByDiscussionRootIn(userBetas);
        assertFalse(solutionBetas.isEmpty());

        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKey));

        assertNull(userAccountManager.findUserAccount(firebaseUid));
        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.COMMENT).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.BETA).isEmpty());
        assertTrue(accountRepository.findById(account.getId()).isEmpty());
    }

    @Test
    @DisplayName("test deleting account with no related data still succeeds")
    void testDeleteAccountWithNoRelatedData() {
        String firebaseUid = "no-related-data-user";
        UserAccount account = creatFakeAccount("no-data-user", "no-data@gmail.com", firebaseUid);

        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.COMMENT).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.BETA).isEmpty());

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

        List<DiscussionRoot> userComments = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.COMMENT
        );
        assertEquals(2, userComments.size());
        assertEquals(2, discussionCommentRepository.findByDiscussionRootIn(userComments).size());
        assertEquals(1, userPerceiveGradeRepository.findByUserAccount(account).size());
        List<DiscussionRoot> userBetas = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.BETA
        );
        assertEquals(2, userBetas.size());
        assertEquals(2, solutionBetaRepository.findByDiscussionRootIn(userBetas).size());

        assertDoesNotThrow(() -> accountService.deleteAccount(firebaseUid));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKeyOne));
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKeyTwo));

        assertNull(userAccountManager.findUserAccount(firebaseUid));
        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.COMMENT).isEmpty());
        assertTrue(userPerceiveGradeRepository.findByUserAccount(account).isEmpty());
        assertTrue(discussionRootRepository.findByUserAccount_AndDiscussionType(account, DiscussionType.BETA).isEmpty());
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

        List<DiscussionRoot> userComments = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.COMMENT
        );
        assertEquals(1, userComments.size());
        DiscussionRoot commentAnchor = userComments.get(0);
        assertTrue(discussionCommentRepository.findByDiscussionRoot(commentAnchor).isPresent());
        discussionCommentRepository.delete(discussionCommentRepository.findByDiscussionRoot(commentAnchor).get());
        assertTrue(discussionCommentRepository.findByDiscussionRoot(commentAnchor).isEmpty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.deleteAccount(firebaseUid)
        );
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

        List<DiscussionRoot> userBetas = discussionRootRepository.findByUserAccount_AndDiscussionType(
                account, DiscussionType.BETA
        );
        assertEquals(1, userBetas.size());
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByDiscussionRootIn(userBetas);
        assertEquals(1, solutionBetas.size());
        solutionBetaRepository.delete(solutionBetas.get(0));
        assertTrue(solutionBetaRepository.findByDiscussionRootIn(userBetas).isEmpty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> accountService.deleteAccount(firebaseUid)
        );
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