package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.DiscussionContentRequest;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.application.support.cloud.GcpFileStorageAdapter;
import app.VBeta.repository.SolutionBetaRepository;
import app.VBeta.api.dto.discussions.PerceiveGradeRequest;
import app.VBeta.api.dto.problems.ClimbingProblemCreationRequest;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.climb.LifecycleStatus;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionComment;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.domain.model.user.UserPerceiveGrade;
import app.VBeta.repository.ClimbingProblemRepository;
import app.VBeta.repository.DiscussionCommentRepository;
import app.VBeta.repository.UserAccountRepository;
import app.VBeta.repository.UserPerceiveGradeRepository;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.application.support.discussion.comment.DiscussionCommentManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import org.junit.jupiter.api.*;
import app.VBeta.config.TestGcpStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class ClimbingProblemModificationTest {
    @Autowired
    private ClimbingWallService climbingWallService;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private WallSectionManager wallSectionManager;

    @Autowired
    private DiscussionCommentManager discussionCommentManager;

    @Autowired
    private DiscussionRootManager discussionRootManager;

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private ProblemDiscussionService problemDiscussionService;
    @Autowired
    private ClimbingProblemRepository climbingProblemRepository;
    @Autowired
    private DiscussionCommentRepository discussionCommentRepository;
    @Autowired
    private UserPerceiveGradeRepository userPerceiveGradeRepository;
    @Autowired
    private SolutionBetaRepository solutionBetaRepository;
    @MockitoBean
    private GcpFileStorageAdapter gcpFileStorageAdapter;

    private final ClimbingProblemCreationRequest request = new ClimbingProblemCreationRequest(
            "Yellow",
            "Yellow two hands start in the corner",
            GradeDefinition.V5,
            null,
            null
    );
    private WallSection createWallSection(){
        WallSectionCreationRequest req  = new WallSectionCreationRequest(
                "Test Wall Section Info",
                "Test Wall Section Name",
                null,
                null
        );

        return wallSectionManager.createNewWallSection(req);
    }

    private void addCommentsToProblem(Long problemId){
        List<UserAccount> accounts = userAccountRepository.findAll();
        assertFalse(accounts.isEmpty(), "There is no accounts inside the database");

        List<DiscussionCommentRequest> requests = new ArrayList<>();
        int counter = 0;

        accounts.forEach(a ->
            requests.add(new DiscussionCommentRequest(
                    problemId,
                    "Test Comment")
            )
        );

        for (int i = 0; i < accounts.size(); i++){
            problemDiscussionService.addComment(accounts.get(i).getFirebaseUid(), requests.get(i));
        }
    }


    @Test
    @Order(1)
    @DisplayName("Test Creating new Climbing Problem")
    void testCreateNewClimbingProblemSuccess(){
        WallSection wall = createWallSection();

        List<ClimbingProblem> problems = climbingProblemManager.getAllProblemsFromWallSection(wall);
        assertTrue(problems.isEmpty());

        climbingWallService.createNewClimbingProblem(wall.getId(), request);

        problems = climbingProblemManager.getAllProblemsFromWallSection(wall);
        assertEquals(1, problems.size());

        ClimbingProblem problem = problems.get(0);
        assertEquals(request.holdColor(), problem.getHoldColor());
        assertEquals(request.info(), problem.getProblemInfo());
        assertEquals(request.assignedGrade(), problem.getClimbingGrade().getGradeDefinition());
        assertEquals(LifecycleStatus.ACTIVE, problem.getProblemStatus());
    }


    @Test
    @Order(2)
    @DisplayName("Test Delete Climbing Problem")
    void testDeletingClimbingProblem(){
        WallSection wall = createWallSection();
        climbingWallService.createNewClimbingProblem(wall.getId(), request);

        ClimbingProblem problem = climbingProblemManager.getAllProblemsFromWallSection(wall).get(0);
        addCommentsToProblem(problem.getId());

        List<DiscussionRoot> comments = discussionRootManager.getDiscussionsByProblemAndType(
                problem, DiscussionType.COMMENT
        );
        assertFalse(comments.isEmpty());

        climbingWallService.deleteClimbingProblem(problem.getId());

        Optional<ClimbingProblem> deletedProblem = climbingProblemRepository.findById(problem.getId());
        assertTrue(deletedProblem.isEmpty());
        comments.forEach(c -> {
            Optional<DiscussionComment> dc = discussionCommentRepository.findByDiscussionRoot(c);
            assertTrue(dc.isEmpty());
            assertNull(discussionRootManager.findDiscussionRootById(c.getDiscussionId()));
        });

        List<ClimbingProblem> problems = climbingProblemManager.getAllProblemsFromWallSection(wall);
        assertTrue(problems.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Test Delete Climbing Problem removes MIXED posts with their comment, beta, and storage object")
    void testDeletingClimbingProblemWithMixedDiscussion(){
        WallSection wall = createWallSection();
        climbingWallService.createNewClimbingProblem(wall.getId(), request);
        ClimbingProblem problem = climbingProblemManager.getAllProblemsFromWallSection(wall).get(0);

        List<UserAccount> accounts = userAccountRepository.findAll();
        assertFalse(accounts.isEmpty(), "There is no accounts inside the database");
        when(gcpFileStorageAdapter.getPublicBucketName()).thenReturn("test-bucket");

        String objectKey = "w1/p" + problem.getId() + "/" + UUID.randomUUID() + ".mp4";
        String publicUrl = "https://storage.googleapis.com/test-bucket/" + objectKey;
        problemDiscussionService.saveUserDiscussionContent(
                accounts.get(0).getFirebaseUid(),
                new DiscussionContentRequest(problem.getId(), "mixed post comment", objectKey, publicUrl)
        );
        addCommentsToProblem(problem.getId());

        List<DiscussionRoot> mixedRoots = discussionRootManager.getDiscussionsByProblemAndType(
                problem, DiscussionType.MIXED
        );
        assertEquals(1, mixedRoots.size());

        climbingWallService.deleteClimbingProblem(problem.getId());

        assertTrue(climbingProblemRepository.findById(problem.getId()).isEmpty());
        verify(gcpFileStorageAdapter).deleteFile(eq("test-bucket"), eq(objectKey));
        assertTrue(solutionBetaRepository.findByVideoURL(publicUrl).isEmpty());
        mixedRoots.forEach(root -> {
            assertTrue(discussionCommentRepository.findByDiscussionRoot(root).isEmpty());
            assertNull(discussionRootManager.findDiscussionRootById(root.getDiscussionId()));
        });
    }

    @Test
    @Order(3)
    @DisplayName("Test for Fail Climbing Problem Creation")
    void testClimbingProblemCreationFailure(){
        RuntimeException ex = assertThrows((RuntimeException.class), () ->
                climbingWallService.createNewClimbingProblem(1234L, request)
        );
    }

    @Test
    @Order(4)
    @DisplayName("Test for creating User Perceive Grade")
    void addUserPerceiveGrade(){
        WallSection wallSection = createWallSection();
        ClimbingProblemResponse response = climbingWallService.createNewClimbingProblem(wallSection.getId(), request);

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(response.problemId());
        assertNotNull(problem);

        List<UserPerceiveGrade> perceiveGrades = userPerceiveGradeRepository.findByClimbingProblem(problem);
        assertTrue(perceiveGrades.isEmpty());

        List<UserAccount> accounts = userAccountRepository.findAll();
        assertFalse(accounts.isEmpty(),
                "Integration test requires at least one User_Account row (seed your test DB)");
        String firebaseUid = accounts.get(0).getFirebaseUid();

        PerceiveGradeRequest gradeRequest = new PerceiveGradeRequest(GradeDefinition.V17);

        problemDiscussionService.addClimbingProblemPerceiveGrade(firebaseUid, problem.getId(), gradeRequest);

        perceiveGrades = userPerceiveGradeRepository.findByClimbingProblem(problem);
        assertEquals(1, perceiveGrades.size());

    }
}
