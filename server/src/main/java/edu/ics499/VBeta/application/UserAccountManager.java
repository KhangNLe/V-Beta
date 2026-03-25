package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.domain.model.Role;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserAccountManager {

    private final UserAccountRepository userAccountRepository;
    private final GymRoleRepository gymRoleRepository;

    public UserAccountManager(
            UserAccountRepository userAccountRepository, GymRoleRepository gymRoleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.gymRoleRepository = gymRoleRepository;
    }

    /**
     * Register a new account; assigns default {@link Role#CLIMBER}. Fails if username already exists.
     */
    public AccountResponse register(AccountRequest request) {
        UserAccount saved = createUserAccount(request.username(), request.email(), request.firebaseUid());
        return toResponse(saved);
    }

    /**
     * Return existing account by Firebase UID, or create one with default role (signup-on-first-login).
     */
    public AccountResponse loginOrRegister(AccountRequest request) {
        UserAccount account = loginUser(request.username(), request.email(), request.firebaseUid());
        return toResponse(account);
    }

    public UserAccount createUserAccount(String username, String email, String firebaseUid) {
        var climberRole =
                gymRoleRepository
                        .findByRoleName(Role.CLIMBER.name())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Gym_Role row missing for "
                                                + Role.CLIMBER.name()
                                                + "; seed the database before registering users."));

        Optional<UserAccount> existingUser = userAccountRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        UserAccount newAccount = new UserAccount();
        newAccount.setUsername(username);
        newAccount.setEmail(email);
        newAccount.setFirebaseUid(firebaseUid);
        newAccount.setGymRole(climberRole);
        return userAccountRepository.save(newAccount);
    }

    public UserAccount loginUser(String username, String email, String firebaseUid) {
        Optional<UserAccount> existingUser = userAccountRepository.findByFirebaseUid(firebaseUid);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        return createUserAccount(username, email, firebaseUid);
    }

    private AccountResponse toResponse(UserAccount u) {
        String roleName = u.getGymRole() != null ? u.getGymRole().getRoleName() : null;
        return new AccountResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirebaseUid(), roleName);
    }
}
