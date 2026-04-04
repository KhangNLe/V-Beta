package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
