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

@Service
@Transactional
public class UserAccountManager {
    private final UserAccountRepository userAccountRepository;
    private final GymRoleRepository gymRoleRepository;

    public UserAccountManager(UserAccountRepository userAccountRepository,
                              GymRoleRepository gymRoleRepository){
        this.userAccountRepository = userAccountRepository;
        this.gymRoleRepository = gymRoleRepository;
    }

    public UserAccount findUserAccount(String firebaseUid){
        Optional<UserAccount> result = userAccountRepository.findByFirebaseUid(firebaseUid);
        return result.orElse(null);
    }

    public UserAccount findUserAccountById(Long userId){
        Optional<UserAccount> result = userAccountRepository.findById(userId);
        return result.orElse(null);
    }

    public UserAccount findUserAccountWithRole(String firebaseUid) {
        Optional<UserAccount> result = userAccountRepository.findByFirebaseUidWithRole(firebaseUid);
        return result.orElse(null);
    }

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
}
