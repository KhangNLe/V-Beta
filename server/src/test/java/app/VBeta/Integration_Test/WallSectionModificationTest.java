package app.VBeta.Integration_Test;

import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.config.TestGcpStorageConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

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
public class WallSectionModificationTest {
    @Autowired
    private ClimbingWallService climbingWallService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private WallSectionManager wallSectionManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    void clearWallSection(){
        List<WallSection> walls = wallSectionManager.getWallSections();
        walls.forEach(w -> climbingWallService.deleteWallSection(w.getId()));
    }

    void testForClearWallSection(){
        List<WallSection> walls = wallSectionManager.getWallSections();
        assertTrue(walls.isEmpty(), "Wall Section supposed to be clear before each test");
    }

    @Test
    @Order(1)
    @DisplayName("Test for Wall Section Reset Success")
    void testWallSectionResetSuccessCase(){

        String setterFirebaseUid = "testFirebaseUid2";
        authorizationService.authorize(setterFirebaseUid, ActionDefinition.RESET_WALL);

        List<WallSection> walls = wallSectionManager.getWallSections();

        assertFalse(walls.isEmpty());

        WallSection wall = walls.get(0);
        List<ClimbingProblem> activeProblems = climbingProblemManager.getAllActiveProblemFromWallSection(wall);
        assertFalse(activeProblems.isEmpty());

        climbingWallService.resetWallSection(wall.getId());

        activeProblems = climbingProblemManager.getAllActiveProblemFromWallSection(wall);
        assertTrue(activeProblems.isEmpty());

        activeProblems = climbingProblemManager.getAllProblemsFromWallSection(wall);
        assertFalse(activeProblems.isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Test Wall Reset Failures")
    void testWallResetFailureCase(){
        String climberFirebaseUid = "testFirebaseUid";

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(climberFirebaseUid, ActionDefinition.RESET_WALL)
        );

        String adminFirebaseUid = "testFirebaseUid3";

        ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(adminFirebaseUid, ActionDefinition.RESET_WALL)
        );

        ex = assertThrows(RuntimeException.class, () ->
                climbingWallService.resetWallSection(123435L)
        );
    }

    @Test
    @Order(3)
    @DisplayName("Test for Wall Creation with correct permission")
    void testForWallCreationSuccess(){
        clearWallSection();
        testForClearWallSection();
        //test admin firebaseUid
        String firebaseUid = "testFirebaseUid3";

        authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_WALL);
        WallSectionCreationRequest req = new WallSectionCreationRequest(
            "Test Wall Section Info",
                "Wall Section Test Name"
        );

        wallSectionManager.createNewWallSection(req);

        List<WallSection> walls = wallSectionManager.getWallSections();
        assertEquals(1, walls.size(), "There should only be 1 wall section exist.");

        WallSection wall = walls.get(0);
        assertEquals(wall.getWallInfo(), req.wallSectionInfo(), "Mismatching info");
        assertEquals(wall.getWallSectionName(), req.wallSectionName(), "Mismatching name");
    }


    @Test
    @Order(4)
    @DisplayName("Test for Wall Creation Failure due to permission")
    void testFailureWallCreationFromPermission(){
        clearWallSection();
        testForClearWallSection();

        //test climber firebaseUid
        String climberFirebaseUid = "testFirebaseUid";

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(climberFirebaseUid, ActionDefinition.CREATE_WALL)
        );

        String setterFirebaseUid = "testFirebaseUid2";

        ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(setterFirebaseUid, ActionDefinition.CREATE_WALL)
        );
    }

    @Test
    @Order(5)
    @DisplayName("Test for Wall Deletion Success")
    void testWallDeletionSuccessCase(){
        clearWallSection();
        testForClearWallSection();
        String adminFirebaseUid = "testFirebaseUid3";

        authorizationService.authorize(adminFirebaseUid, ActionDefinition.DELETE_WALL);

        WallSectionCreationRequest req = new WallSectionCreationRequest(
                "Test Wall Section Info",
                "Wall Section Test Name"
        );

        wallSectionManager.createNewWallSection(req);

        List<WallSection> walls = wallSectionManager.getWallSections();
        assertEquals(1, walls.size());

        WallSection wall = walls.get(0);
        climbingWallService.deleteWallSection(wall.getId());

        testForClearWallSection();
    }

    @Test
    @Order(6)
    @DisplayName("Test for Wall Deletion Error")
    void testForWallDeletionErrorCase(){
        clearWallSection();
        testForClearWallSection();

        String climberFirebaseUid = "testFirebaseUid";

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(climberFirebaseUid, ActionDefinition.DELETE_WALL)
        );

        String setterFirebaseUid = "testFirebaseUid2";

        ex = assertThrows(RuntimeException.class, () ->
                authorizationService.authorize(setterFirebaseUid, ActionDefinition.DELETE_WALL)
        );

        ex = assertThrows(RuntimeException.class, () ->
                climbingWallService.resetWallSection(12345L)
        );
    }
}
