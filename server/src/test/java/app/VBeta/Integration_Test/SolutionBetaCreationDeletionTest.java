package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.video.SolutionBetaCreateRequest;
import app.VBeta.api.dto.discussions.video.SolutionBetaDeletionRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.discussion.beta.GcpFileStorageAdapter;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.SolutionBetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
        "spring.datasource.username=${SQL_USERNAME:postgres}",
        "spring.datasource.password=${SQL_PASSWORD:postgres}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
public class SolutionBetaCreationDeletionTest {
    private static final String USER_DELETED_OWN_DISCUSSION = "User deleted their own discussion";
    private static final String ADMIN_FORCED_DELETE_DISCUSSION = "Admin forced delete the discussion";


    @Autowired
    private ProblemDiscussionService problemDiscussionService;

    @Autowired
    private ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    @Autowired
    private SolutionBetaRepository solutionBetaRepository;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private UserAccountManager userAccountManager;

    @MockitoBean
    private GcpFileStorageAdapter gcpFileStorageAdapter;
    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Test
    @DisplayName("test for creating new solution beta after file been stored in cloud bucket")
    void testSolutionBetaDataCreation(){
        SolutionBetaCreateRequest testRequest = new SolutionBetaCreateRequest(
                1L,
                "testvid.mp4",
                "https://storage.googleapis.com/test-bucket/testvid.mp4"
        );

        String testFirebaseUid = "testFirebaseUid";
        UserDiscussionData newDataResponse = problemDiscussionService.saveSolutionBeta(testRequest, testFirebaseUid);

        UserAccount user = userAccountManager.findUserAccount(testFirebaseUid);
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(testRequest.problemId());

        List<DiscussionRoot> userBetas = findDiscussionsForUserProblem(user, problem, DiscussionType.BETA);

        assertFalse(userBetas.isEmpty());
        assertEquals(testRequest.videoURL(), newDataResponse.discussionContent());
        assertTrue(checkForSolutionBetaExist(userBetas, testRequest.objectFileName(), testRequest.videoURL()));
    }

    @Test
    @DisplayName("test for fail creation of solution beta from archive climbing problem")
    void testFailSolutionBetaCreation(){
        SolutionBetaCreateRequest testRequest = new SolutionBetaCreateRequest(
                3L,
                "testvid.mp4",
                "https://storage.googleapis.com/test-bucket/testvid.mp4"
        );
        String testFirebaseUid = "testFirebaseUid";

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.saveSolutionBeta(testRequest, testFirebaseUid)
        );
    }

    @Test
    @DisplayName("solution beta owner delete soft-deletes discussion root and keeps storage/DB rows")
    void testDeleteUserSolutionBeta() {
        String firebaseUid = "testFirebaseUid";
        Long problemId = 2L;
        String objectKey = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrl = "https://storage.googleapis.com/test-bucket/" + objectKey;
        SolutionBetaDeletionRequest deletePayload = createFakeSolutionBeta(
                firebaseUid, objectKey, publicUrl, problemId, USER_DELETED_OWN_DISCUSSION);

        UserAccount userAccount = userAccountManager.findUserAccount(firebaseUid);
        ClimbingProblem climbingProblem = climbingProblemManager.getActiveProblem(problemId);

        problemDiscussionService.softDeleteUserSolutionBeta(deletePayload, firebaseUid);
        verify(gcpFileStorageAdapter, never()).deleteFile(eq("test-bucket"), eq(objectKey));
        assertTrue(solutionBetaRepository.findByVideoURL(publicUrl).isPresent());

        DiscussionRoot deletedRoot = discussionRootRepository.findById(deletePayload.discussionId()).orElseThrow();
        assertNotNull(deletedRoot.getDeletedAt());
        assertEquals(USER_DELETED_OWN_DISCUSSION, deletedRoot.getDeletedReason());
        assertEquals(userAccount.getId(), deletedRoot.getDeletedBy().getId());
        assertTrue(solutionBetaRepository.findByDiscussionRoot(deletedRoot).isPresent());
        assertTrue(climbingProblemDiscussionManager.getCommentsForProblem(climbingProblem).stream()
                .noneMatch(item -> item.discussionId().equals(deletePayload.discussionId())));
    }

    @Test
    @DisplayName("test for fail deletion due to unexisting object file name")
    void testFailSolutionBetaDeletion(){
        SolutionBetaDeletionRequest request = new SolutionBetaDeletionRequest(
                1L,
                1L,
                1L,
                "testSolutionBeta.mp4",
                USER_DELETED_OWN_DISCUSSION
        );

        String firebaseUid = "testFirebaseUid";

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.softDeleteUserSolutionBeta(request, firebaseUid)
        );
    }

    @Test
    @DisplayName("test for fail delete solution beta due to wrong owner/authorization")
    void testFailDeletionForWrongAuthor(){
        SolutionBetaDeletionRequest request = new SolutionBetaDeletionRequest(
                1L,
                1L,
                1L,
                "testSolutionBeta.mp4",
                USER_DELETED_OWN_DISCUSSION
        );

        String firebaseUid = "testFirebaseUid2";
        Long problemId = 1L;

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                problemDiscussionService.softDeleteUserSolutionBeta(request, firebaseUid)
        );
    }

    @Test
    @DisplayName("test for success solution beta deletion from admin account for different user solution beta")
    void testSuccessDeletionFromAdmin(){
        String firebaseUid = "testFirebaseUid";
        Long problemId = 1L;
        String objectKey = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        String publicUrl = "https://storage.googleapis.com/test-bucket/" + objectKey;
        SolutionBetaDeletionRequest fakeData = createFakeSolutionBeta(
                firebaseUid, objectKey, publicUrl, problemId, ADMIN_FORCED_DELETE_DISCUSSION);

        String adminFirebaseUid = "testFirebaseUid3";
        UserAccount adminAccount = userAccountManager.findUserAccount(adminFirebaseUid);
        problemDiscussionService.softDeleteUserSolutionBeta(fakeData, adminFirebaseUid);
        verify(gcpFileStorageAdapter, never()).deleteFile(eq("test-bucket"), eq(objectKey));
        assertTrue(solutionBetaRepository.findByVideoURL(publicUrl).isPresent());

        DiscussionRoot deletedRoot = discussionRootRepository.findById(fakeData.discussionId()).orElseThrow();
        assertNotNull(deletedRoot.getDeletedAt());
        assertEquals(ADMIN_FORCED_DELETE_DISCUSSION, deletedRoot.getDeletedReason());
        assertEquals(adminAccount.getId(), deletedRoot.getDeletedBy().getId());
    }

    @Test
    @DisplayName("test for failure from unknown userId")
    void testForFailDeletionFromUnknownUserID(){
        SolutionBetaDeletionRequest request = new SolutionBetaDeletionRequest(
                123454L,
                1L,
                1L,
                "testSolutionBeta.mp4",
                ADMIN_FORCED_DELETE_DISCUSSION
        );
        String adminFirebaseUid = "testFirebaseUid3";

        RuntimeException ex = assertThrows( RuntimeException.class, () ->
                problemDiscussionService.softDeleteUserSolutionBeta(request, adminFirebaseUid)
        );
    }

    private SolutionBetaDeletionRequest createFakeSolutionBeta(String firebaseUid, String objectKey, String publicUrl,
                                                               Long problemId, String deleteReason){

        when(gcpFileStorageAdapter.getPublicBucketName()).thenReturn("test-bucket");

        SolutionBetaCreateRequest createRequest = new SolutionBetaCreateRequest(problemId, objectKey, publicUrl);
        UserDiscussionData saved = problemDiscussionService.saveSolutionBeta(createRequest, firebaseUid);
        SolutionBeta solutionBeta = solutionBetaRepository.findByVideoURL(publicUrl).orElseThrow();
        Long discussionId = solutionBeta.getDiscussionRoot().getDiscussionId();

        return new SolutionBetaDeletionRequest(
                saved.userId(),
                problemId,
                discussionId,
                saved.discussionContent(),
                deleteReason
        );
    }

    private boolean checkForSolutionBetaExist(List<DiscussionRoot> betas, String objectFileName, String publicUrl){
        for(DiscussionRoot b : betas){
            Optional<SolutionBeta> sol = solutionBetaRepository.findByDiscussionRoot(b);
            if (sol.isEmpty()) continue;
            SolutionBeta solBeta = sol.get();
            if (solBeta.getVideoURL().equals(publicUrl) && solBeta.getBetaName().equals(objectFileName)){
                return true;
            }
        }
        return false;
    }

    private List<DiscussionRoot> findDiscussionsForUserProblem(
            UserAccount user, ClimbingProblem problem, DiscussionType discussionType
    ) {
        return discussionRootRepository.findByUserAccount_AndDiscussionType(user, discussionType).stream()
                .filter(discussionRoot -> discussionRoot.getProblem().getId().equals(problem.getId()))
                .toList();
    }
}
