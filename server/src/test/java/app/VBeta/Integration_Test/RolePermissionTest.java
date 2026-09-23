package app.VBeta.Integration_Test;

import app.VBeta.application.support.account.RoleBasedAuthenticationManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.repository.RolePermissionRepository;
import app.VBeta.config.TestGcpStorageConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class RolePermissionTest {
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleBasedAuthenticationManager roleBasedAuthenticationManager;

    private final List<ActionDefinition> climberActions = List.of(
            ActionDefinition.CREATE_BETA,
            ActionDefinition.DELETE_BETA,
            ActionDefinition.CREATE_COMMENT,
            ActionDefinition.DELETE_COMMENT,
            ActionDefinition.GRADE_PROBLEM
    );

    private final List<ActionDefinition> setterActions = List.of(
            ActionDefinition.CREATE_PROBLEM,
            ActionDefinition.RESET_WALL,
            ActionDefinition.DELETE_PROBLEM,
            ActionDefinition.UPLOAD_PROBLEM_IMAGE
    );

    private final List<ActionDefinition> adminActions = List.of(
            ActionDefinition.CREATE_WALL,
            ActionDefinition.DELETE_WALL,
            ActionDefinition.CHANGE_ROLE,
            ActionDefinition.VIEW_ACCOUNTS,
            ActionDefinition.VIEW_REPORTS,
            ActionDefinition.VIEW_APPEALS,
            ActionDefinition.VIEW_MODERATION_LOGS,
            ActionDefinition.MODERATE_APPEAL,
            ActionDefinition.MODERATE_REPORT,
            ActionDefinition.UPLOAD_WALL_IMAGE

    );

    @Test
    @DisplayName("Test Climber allowable action permission")
    void testClimberActionPermission(){
        RoleType climber = RoleType.CLIMBER;

        climberActions.forEach(a -> {
            assertTrue(roleBasedAuthenticationManager.isPermit(climber, a));
        });

        setterActions.forEach(a -> {
            assertFalse(roleBasedAuthenticationManager.isPermit(climber, a));
        });

        adminActions.forEach(a -> {
            assertFalse(roleBasedAuthenticationManager.isPermit(climber, a));
        });
    }

    @Test
    @DisplayName("Test Setter allowable action")
    void testSetterActionPermission(){
        RoleType setter = RoleType.SETTER;

        climberActions.forEach(a ->{
            assertTrue(roleBasedAuthenticationManager.isPermit(setter, a));
        });

        setterActions.forEach(a ->{
            assertTrue(roleBasedAuthenticationManager.isPermit(setter, a));
        });

        adminActions.forEach(a ->{
            assertFalse(roleBasedAuthenticationManager.isPermit(setter, a));
        });
    }

    @Test
    @DisplayName("Test Admin allowable action permissions")
    void testAdminActionPermission(){
        RoleType admin = RoleType.ADMIN;

        climberActions.forEach(a ->{
            assertTrue(roleBasedAuthenticationManager.isPermit(admin, a));
        });

        setterActions.forEach(a ->{
            assertFalse(roleBasedAuthenticationManager.isPermit(admin, a));
        });

        adminActions.forEach(a ->{
            assertTrue(roleBasedAuthenticationManager.isPermit(admin, a));
        });
    }
}
