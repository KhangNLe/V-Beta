package app.VBeta.Integration_Test;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.GymRoleRepository;
import app.VBeta.repository.UserAccountRepository;
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

import com.google.cloud.storage.Storage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
  properties = {
    "spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:v_beta_test}",
    "spring.datasource.username=${SQL_USERNAME:postgres}",
    "spring.datasource.password=${SQL_PASSWORD:postgres}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
  }
)
public class AccountViewAllTest {

  @Autowired
  private AccountService accountService;

  @Autowired
  private AuthorizationService authorizationService;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private GymRoleRepository gymRoleRepository;

  @MockitoBean
  private Storage storage;

  @Test
  @DisplayName("Test admin can view all accounts")
  void testAdminCanViewAllAccounts() {
    // Authorize as admin
    String adminFirebaseUid = "testFirebaseUid3";
    authorizationService.authorize(adminFirebaseUid, ActionDefinition.VIEW_ACCOUNTS);

    // Create some test accounts to ensure we have data
    createTestAccount("user1", "user1@test.com", "firebase1", RoleType.CLIMBER);
    createTestAccount("user2", "user2@test.com", "firebase2", RoleType.SETTER);
    createTestAccount("user3", "user3@test.com", "firebase3", RoleType.ADMIN);

    // Get all accounts
    List<UserAccountDTO> accounts = accountService.getAllAccounts();

    // Verify we got accounts back
    assertNotNull(accounts);
    assertTrue(accounts.size() >= 3, "Should have at least the 3 test accounts we created");

    // Verify each account has the expected fields
    for (UserAccountDTO account : accounts) {
      assertNotNull(account.userId());
      assertNotNull(account.username());
      assertNotNull(account.email());
      assertNotNull(account.role());
    }
  }

  @Test
  @DisplayName("Test climber cannot view all accounts")
  void testClimberCannotViewAllAccounts() {
    String climberFirebaseUid = "testFirebaseUid";

    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> authorizationService.authorize(climberFirebaseUid, ActionDefinition.VIEW_ACCOUNTS)
    );
  }

  @Test
  @DisplayName("Test setter cannot view all accounts")
  void testSetterCannotViewAllAccounts() {
    String setterFirebaseUid = "testFirebaseUid2";

    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> authorizationService.authorize(setterFirebaseUid, ActionDefinition.VIEW_ACCOUNTS)
    );
  }

  @Test
  @DisplayName("Test unauthenticated user cannot view all accounts")
  void testUnauthenticatedUserCannotViewAllAccounts() {
    String fakeFirebaseUid = "nonexistentFirebaseUid";

    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> authorizationService.authorize(fakeFirebaseUid, ActionDefinition.VIEW_ACCOUNTS)
    );
  }

  @Test
  @DisplayName("Test getAllAccounts returns correct account data")
  void testGetAllAccountsReturnsCorrectData() {
    // Authorize as admin
    String adminFirebaseUid = "testFirebaseUid3";
    authorizationService.authorize(adminFirebaseUid, ActionDefinition.VIEW_ACCOUNTS);

    // Create a specific test account
    UserAccount testAccount = createTestAccount("testuser", "test@example.com", "testfirebase", RoleType.CLIMBER);

    // Get all accounts
    List<UserAccountDTO> accounts = accountService.getAllAccounts();

    // Find our test account in the results
    UserAccountDTO foundAccount = accounts.stream()
      .filter(account -> account.userId().equals(testAccount.getId()))
      .findFirst()
      .orElse(null);

    assertNotNull(foundAccount, "Test account should be in the results");
    assertEquals(testAccount.getUsername(), foundAccount.username());
    assertEquals(testAccount.getEmail(), foundAccount.email());
    assertEquals(testAccount.getGymRole().getRoleType().name(), foundAccount.role());
  }

  private UserAccount createTestAccount(
    String username,
    String email,
    String firebaseUid,
    RoleType roleType
  ) {
    GymRole role = gymRoleRepository.findByRoleType(roleType).orElseThrow();

    UserAccount account = new UserAccount();
    account.setUsername(username);
    account.setEmail(email);
    account.setFirebaseUid(firebaseUid);
    account.setGymRole(role);

    return userAccountRepository.save(account);
  }
}