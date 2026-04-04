package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.GymRole;
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
        if (result.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account does not exist.");
        }
        return result.get();
    }

    public UserAccount findUserAccountById(Long userId){
        Optional<UserAccount> result = userAccountRepository.findById(userId);
        if (result.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account does not exist.");
        }
        return result.get();
    }

    public void createNewAccount(String userName, String email, String firebaseUid){
        GymRole role =
    }
}
