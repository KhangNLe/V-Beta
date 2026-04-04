package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
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
    private AccountService accountService;

    @Autowired
    private GymRoleRepository gymRoleRepository;


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
}