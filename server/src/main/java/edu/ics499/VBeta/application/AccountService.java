package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountService {
    private final UserAccountManager userAccountManager;

    public AccountService(UserAccountManager userAccountManager){
        this.userAccountManager = userAccountManager;
    }

    public AccountResponse loginAccount(String username, String email, String firebaseUid) {
        UserAccount account = getUserInfo(username, email, firebaseUid);
        return responseInfo(account);
    }

    // This method changes the role of a user account. It takes the user ID and the new role type as parameters. It returns an AccountResponse with the updated account information after the role change. If the user account is not found, it throws a 404 NOT FOUND error. If the specified role type does not exist in the database, it throws a 400 BAD REQUEST error.
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
