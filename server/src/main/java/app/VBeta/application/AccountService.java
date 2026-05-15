package app.VBeta.application;

import app.VBeta.api.dto.AccountResponse;
import app.VBeta.application.support.account.AccountDeletionManager;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.GymRole;
import app.VBeta.domain.model.RoleType;
import app.VBeta.domain.model.UserAccount;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code AccountService} is the application-level entry point for account lifecycle behavior.
 * It resolves users by Firebase identity, performs account bootstrap/upsert logic, and handles
 * account deletion and role-change workflows.
 * <p>
 * This service delegates persistence concerns to {@link UserAccountManager} and returns API-facing
 * data using {@link AccountResponse}.
 */
@Service
@Transactional
public class AccountService {
    private final UserAccountManager userAccountManager;
    private final AccountDeletionManager accountDeletionManager;

    /**
     * Constructs a new {@code AccountService} with required account management dependency.
     *
     * @param userAccountManager manager responsible for account lookup and creation
     * @param accountDeletionManager manager responsible for transactional account teardown
     */
    public AccountService(UserAccountManager userAccountManager,
                          AccountDeletionManager accountDeletionManager){
        this.userAccountManager = userAccountManager;
        this.accountDeletionManager = accountDeletionManager;
    }

    /**
     * Retrieves all user accounts.
     *
     * @return list of all account responses
     */
    public List<AccountResponse> getAllAccounts() {
        List<UserAccount> accounts = userAccountManager.findAllUserAccounts();
        return accounts.stream()
                .map(this::responseInfo)
                .collect(Collectors.toList());
    }

    /**
     * Logs in an account by Firebase identity, creating one if missing.
     *
     * @param username requested username for first-time account creation
     * @param email requested email for first-time account creation
     * @param firebaseUid Firebase UID from authenticated identity token
     * @return normalized account response payload
     */
    public AccountResponse loginAccount(String username, String email, String firebaseUid) {
        UserAccount account = getUserInfo(username, email, firebaseUid);
        return responseInfo(account);
    }

    /**
     * Deletes an account by Firebase UID when the account exists.
     *
     * @param firebaseUid Firebase UID of the account to delete
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no account matches the UID
     */
    public void deleteAccount(String firebaseUid){
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        if (account == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The request account is currently does not exist within the database."
            );
        }
        accountDeletionManager.deleteAllUserRelatedDiscussion(account);
    }

    /**
     * Changes the role assigned to a target user account.
     *
     * @param userId identifier of the user account to update
     * @param roleType target role type to assign
     * @return account response containing the updated role information
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when the user account does not exist
     * @throws ResponseStatusException with {@link HttpStatus#BAD_REQUEST} when the requested role type is unknown
     */
    public AccountResponse changeUserRole(Long userId, RoleType roleType) {
        UserAccount account = userAccountManager.findUserAccountById(userId);

        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found with ID: " + userId);
        }

        GymRole role = userAccountManager.findGymRole(roleType);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role type not found: " + roleType);
        }
        account.setGymRole(role);

        UserAccount savedAccount = userAccountManager.saveUserAccount(account);

        return responseInfo(savedAccount);
    }

    private UserAccount getUserInfo(String username, String email, String firebaseUid) {
        UserAccount account = userAccountManager.findUserAccount(firebaseUid);
        return (account == null)? userAccountManager.createNewAccount(username, email, firebaseUid)
                : account;
    }

    private AccountResponse responseInfo(UserAccount u) {
        String roleName = u.getGymRole() != null ? u.getGymRole().getRoleType().name() : null;
        return new AccountResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirebaseUid(), roleName);
    }
}
