package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.UserAccountManager;
import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.Role;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
public class AccountControllerTest {
    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private GymRoleRepository gymRoleRepository;


    @Test
    void testAccountConnection() throws Exception {
        // Ensure prerequisite role exists for account creation path.
        if (gymRoleRepository.findByRoleType(Role.CLIMBER.name()).isEmpty()) {
            GymRole gymRole = new GymRole();
            gymRole.setRoleType(Role.CLIMBER.name());
            gymRoleRepository.save(gymRole);
        }

        AccountRequest req = new AccountRequest(
            "testUser",
            "testUser@gmail.com",
            "testFirebaseUid"
        );

        AccountResponse resp = userAccountManager.loginAccount(req);
        assertTrue(accountRepository.findByFirebaseUid(req.firebaseUid()).isPresent());
        assertNotNull(resp);
        assertNotNull(resp.id());
        assertEquals(req.firebaseUid(), resp.firebaseUid());
        assertEquals("testUser", resp.username());
        assertEquals("testUser@gmail.com", resp.email());
    }
}