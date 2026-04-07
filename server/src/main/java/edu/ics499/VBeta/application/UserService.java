package edu.ics499.VBeta.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.UserAccountRepository;

// Service responsible for handling user-related operations, such as retrieving user accounts based on Firebase UIDs. It interacts with the UserAccountRepository to access user data from the database.
@Service
@Transactional
public class UserService {
   
    private final UserAccountRepository userAccountRepository;

    // Constructor for UserService, which takes a UserAccountRepository as a dependency. This repository is used to access user account data from the database.
    public UserService(UserAccountRepository userAccountRepository) {
      this.userAccountRepository = userAccountRepository;
    }
    // Method to retrieve a UserAccount based on the provided Firebase UID. It uses the UserAccountRepository to find the user account associated with the given UID. If no user is found, it throws an IllegalStateException.
    public UserAccount getFirebaseUid(String firebaseUid) {
        return userAccountRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new IllegalStateException("User not found for UID: " + firebaseUid));
    }
  
}
