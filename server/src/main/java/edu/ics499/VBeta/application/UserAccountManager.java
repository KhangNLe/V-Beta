package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.domain.model.GymRole;
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

    public AccountResponse loginAccount(AccountRequest request) {
        UserAccount account = getUserInfo(request.username(), request.email(), request.firebaseUid());
        return responseInfo(account);
    }

    private UserAccount createUserAccount(String username, String email, String firebaseUid) {
        Optional<GymRole> climber = gymRoleRepository.findByRoleType(Role.CLIMBER.name());
        if (climber.isEmpty()) {
            throw new IllegalStateException(
                "Gym_Role missing data for " + Role.CLIMBER.name() + "; please contact the developer."
            );
        }

        Optional<UserAccount> existingUser = userAccountRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        UserAccount newAccount = new UserAccount();
        newAccount.setUsername(username);
        newAccount.setEmail(email);
        newAccount.setFirebaseUid(firebaseUid);
        newAccount.setGymRole(climber.get());
        return userAccountRepository.save(newAccount);
    }

    private UserAccount getUserInfo(String username, String email, String firebaseUid) {
        Optional<UserAccount> existingUser = userAccountRepository.findByFirebaseUid(firebaseUid);
        return existingUser.orElseGet(() -> createUserAccount(username, email, firebaseUid));
    }

    private AccountResponse responseInfo(UserAccount u) {
        String roleName = u.getGymRole() != null ? u.getGymRole().getRoleType() : null;
        return new AccountResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirebaseUid(), roleName);
    }
}
