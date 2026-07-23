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
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
        "spring.datasource.username=${SQL_USERNAME:postgres}",
        "spring.datasource.password=${SQL_PASSWORD:postgres}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
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
            ActionDefinition.DELETE_PROBLEM
    );

    private final List<ActionDefinition> adminActions = List.of(
            ActionDefinition.CREATE_WALL,
            ActionDefinition.DELETE_WALL,
            ActionDefinition.CHANGE_ROLE,
            ActionDefinition.VIEW_ACCOUNTS
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
