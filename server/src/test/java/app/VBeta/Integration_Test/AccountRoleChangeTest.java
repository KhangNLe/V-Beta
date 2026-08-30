package app.VBeta.Integration_Test;

import static org.junit.jupiter.api.Assertions.*;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class AccountRoleChangeTest {

  @Autowired
  private AccountService accountService;

  @Autowired
  private AuthorizationService authorizationService;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private GymRoleRepository gymRoleRepository;

  @Test
  @DisplayName("Test admin can promote user account to setter")
  void testAdminCanPromoteUserAccountToSetter() {
    String adminFirebaseUid = "testFirebaseUid3";

    authorizationService.authorize(
      adminFirebaseUid,
      ActionDefinition.CHANGE_ROLE
    );

    UserAccount targetAccount = createTestAccount(
      "promoteUser",
      "promoteUser@test.com",
      "promoteUserFirebaseUid",
      RoleType.CLIMBER
    );

    UserAccountDTO response = accountService.changeUserRole(
      targetAccount.getId(),
      RoleType.SETTER
    );

    UserAccount updatedAccount = userAccountRepository
      .findById(targetAccount.getId())
      .orElseThrow();

    assertNotNull(response);
    assertEquals(targetAccount.getId(), response.userId());
    assertEquals(RoleType.SETTER.name(), response.role());
    assertEquals(RoleType.SETTER, updatedAccount.getGymRole().getRoleType());
  }

  @Test
  @DisplayName("Test admin can demote user account to climber")
  void testAdminCanDemoteUserAccountToClimber() {
    String adminFirebaseUid = "testFirebaseUid3";

    authorizationService.authorize(
      adminFirebaseUid,
      ActionDefinition.CHANGE_ROLE
    );

    UserAccount targetAccount = createTestAccount(
      "demoteUser",
      "demoteUser@test.com",
      "demoteUserFirebaseUid",
      RoleType.SETTER
    );

    UserAccountDTO response = accountService.changeUserRole(
      targetAccount.getId(),
      RoleType.CLIMBER
    );

    UserAccount updatedAccount = userAccountRepository
      .findById(targetAccount.getId())
      .orElseThrow();

    assertNotNull(response);
    assertEquals(targetAccount.getId(), response.userId());
    assertEquals(RoleType.CLIMBER.name(), response.role());
    assertEquals(RoleType.CLIMBER, updatedAccount.getGymRole().getRoleType());
  }

  @Test
  @DisplayName("Test climber cannot change user account role")
  void testClimberCannotChangeUserAccountRole() {
    String climberFirebaseUid = "testFirebaseUid";

    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () ->
        authorizationService.authorize(
          climberFirebaseUid,
          ActionDefinition.CHANGE_ROLE
        )
    );
  }

  @Test
  @DisplayName("Test setter cannot change user account role")
  void testSetterCannotChangeUserAccountRole() {
    String setterFirebaseUid = "testFirebaseUid2";

    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () ->
        authorizationService.authorize(
          setterFirebaseUid,
          ActionDefinition.CHANGE_ROLE
        )
    );
  }

  @Test
  @DisplayName("Test role change fails when target user account does not exist")
  void testRoleChangeFailsWhenTargetUserAccountDoesNotExist() {
    RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> accountService.changeUserRole(-1L, RoleType.SETTER)
    );
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
