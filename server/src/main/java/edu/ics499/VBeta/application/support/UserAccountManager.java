package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.GymRoleRepository;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * {@code UserAccountManager} provides account lookup and account creation operations.
 * <p>
 * It is responsible for assigning the default {@link RoleType#CLIMBER} role to new users
 * and exposing account retrieval/removal/update methods used by service-level authorization
 * and profile administration flows.
 */
@Service
@Transactional
public class UserAccountManager {
    private final UserAccountRepository userAccountRepository;
    private final GymRoleRepository gymRoleRepository;

    /**
     * Constructs a new {@code UserAccountManager} with user and role repositories.
     *
     * @param userAccountRepository repository for user account entities
     * @param gymRoleRepository repository for role entities
     */
    public UserAccountManager(UserAccountRepository userAccountRepository,
                              GymRoleRepository gymRoleRepository){
        this.userAccountRepository = userAccountRepository;
        this.gymRoleRepository = gymRoleRepository;
    }

    /**
     * Finds an account by Firebase UID.
     *
     * @param firebaseUid Firebase UID to search
     * @return matching account or {@code null} when missing
     */
    public UserAccount findUserAccount(String firebaseUid){
        Optional<UserAccount> result = userAccountRepository.findByFirebaseUid(firebaseUid);
        return result.orElse(null);
    }

    /**
     * Finds an account by internal database identifier.
     *
     * @param userId account identifier
     * @return matching account or {@code null} when missing
     */
    public UserAccount findUserAccountById(Long userId){
        Optional<UserAccount> result = userAccountRepository.findById(userId);
        return result.orElse(null);
    }

    /**
     * Finds an account with role eager-loaded for authorization flows.
     *
     * @param firebaseUid Firebase UID to search
     * @return matching account with role or {@code null} when missing
     */
    public UserAccount findUserAccountWithRole(String firebaseUid) {
        Optional<UserAccount> result = userAccountRepository.findByFirebaseUidWithRole(firebaseUid);
        return result.orElse(null);
    }

    /**
     * Creates and stores a new account with the default climber role.
     *
     * @param userName username to assign
     * @param email email to assign
     * @param firebaseUid Firebase UID to assign
     * @return persisted account
     */
    public UserAccount createNewAccount(String userName, String email, String firebaseUid){
        Optional<GymRole> role = gymRoleRepository.findByRoleType(RoleType.CLIMBER);
        if (role.isEmpty()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Service error while creating new account, please contact the developer for this.");
        }
        UserAccount newAccount = new UserAccount();
        newAccount.setGymRole(role.get());
        newAccount.setUsername(userName);
        newAccount.setEmail(email);
        newAccount.setFirebaseUid(firebaseUid);
        return userAccountRepository.save(newAccount);
    }

    /**
     * Deletes an existing account entity.
     *
     * @param account account entity to remove
     */
    public void removeAccount(UserAccount account){
        userAccountRepository.delete(account);
    }

    /**
     * Persists account changes to storage.
     *
     * @param userAccount account entity to save
     * @return saved account entity
     */
    public UserAccount saveUserAccount(UserAccount userAccount) {
        return userAccountRepository.save(userAccount);
    }

    /**
     * Resolves a persisted gym role by role type.
     *
     * @param roleType role type to resolve
     * @return matching gym role or {@code null} when missing
     */
    public GymRole findGymRole(RoleType roleType) {
        Optional<GymRole> result = gymRoleRepository.findByRoleType(roleType);

        return result.orElse(null);
    }

}
