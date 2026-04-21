package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3307}/${MYSQL_TEST_DB:V_Beta_Test}",
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
    @Autowired
    private UserAccountManager userAccountManager;


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