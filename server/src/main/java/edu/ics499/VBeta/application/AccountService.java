package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@code AccountService} is the application-level entry point for account login/upsert behavior.
 * It resolves a user by Firebase identity and creates a new account when no matching record exists.
 * <p>
 * This service delegates persistence concerns to {@link UserAccountManager} and returns API-facing
 * data using {@link AccountResponse}. It also handles account removal requests for an authenticated
 * identity key.
 */
@Service
@Transactional
public class AccountService {
    private final UserAccountManager userAccountManager;

    /**
     * Constructs a new {@code AccountService} with required account management dependency.
     *
     * @param userAccountManager manager responsible for account lookup and creation
     */
    public AccountService(UserAccountManager userAccountManager){
        this.userAccountManager = userAccountManager;
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
        userAccountManager.removeAccount(account);
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
