package edu.ics499.VBeta.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.ics499.VBeta.domain.model.ActionDefinition;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;

// Service responsible for handling authorization logic based on user roles and defined actions in the system. It checks if a user with a given Firebase UID has the necessary permissions to perform a specific action.
@Service
public class AuthorizationService {

  private final UserAccountManager userAccountManager;
  private final RoleBasedAuthenticationManager roleBasedAuthenticationManager;

  public AuthorizationService(
      UserAccountManager userAccountManager, RoleBasedAuthenticationManager roleBasedAuthenticationManager
  ) {
    this.userAccountManager = userAccountManager;
    this.roleBasedAuthenticationManager = roleBasedAuthenticationManager;
  }

  // Method to authorize a user based on their Firebase UID and the action they are trying to perform. It checks the user's role and permissions, and throws a 403 Forbidden response if the user is not authorized to perform the action.
  public void authorize(String firebaseUid, ActionDefinition action) {
    UserAccount user = userAccountManager.getFirebaseUid(firebaseUid);

    if (user.getGymRole() == null || user.getGymRole().getRoleType() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have a valid role assigned");
    }

    RoleType roleType = user.getGymRole().getRoleType();

    // Check if the user's role has permission to perform the specified action using the RoleBasedAuthenticationManager
    boolean allowed = roleBasedAuthenticationManager.isPermit(roleType, action);

    // If the user's role does not have permission to perform the action, throw a 403 Forbidden response
    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role " + roleType.name() +  " is not allowed to perform action " + action.name());
    }
  }
}
