package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.ClimbingWallService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.application.support.*;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.*;
import org.junit.jupiter.api.*;
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
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
        "spring.datasource.username=${SQL_USERNAME:khang}",
        "spring.datasource.password=${SQL_PASSWORD:}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
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

    private final ClimbingProblemCreationRequest request = new ClimbingProblemCreationRequest(
            "Yellow",
            "Yellow two hands start in the corner",
            GradeDefinition.V5
    );
    private WallSection createWallSection(){
        WallSectionCreationRequest req  = new WallSectionCreationRequest(
                "Test Wall Section Info",
                "Test Wall Section Name"
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
    @Order(3)
    @DisplayName("Test for Fail Climbing Problem Creation")
    void testClimbingProblemCreationFailure(){
        ResponseStatusException ex = assertThrows((ResponseStatusException.class), () ->
                climbingWallService.createNewClimbingProblem(1234L, request)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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
