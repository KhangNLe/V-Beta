package edu.ics499.VBeta.application;

import edu.ics499.VBeta.application.support.RoleBasedAuthenticationManager;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Service responsible for handling authorization logic based on user roles and permissions. It interacts with the RoleBasedAuthenticationManager to determine if a user has the necessary permissions to perform a specific action. It also retrieves user information using the UserService to check their assigned role and permissions.
@Service
public class AuthorizationService {

    private final RoleBasedAuthenticationManager roleBasedAuthenticationManager;
    private final UserService userService;

    public AuthorizationService(
            RoleBasedAuthenticationManager roleBasedAuthenticationManager,
            UserService userService
    ) {
        this.roleBasedAuthenticationManager = roleBasedAuthenticationManager;
        this.userService = userService;
    }


    // Method to retrieve the authenticated user's Firebase UID from the security context. It checks if the user is authenticated and if the principal contains a valid Firebase UID. If any of these checks fail, it throws an appropriate ResponseStatusException with an HTTP status code indicating the error.
    public String getAuthenticatedFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Firebase UID in authentication token");
        }

        String firebaseUid = String.valueOf(principal);
        if (firebaseUid.isBlank() || "anonymousUser".equals(firebaseUid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Firebase UID in authentication token");
        }

        return firebaseUid;
    }


    // Method to authorize the currently authenticated user for a specific action. It retrieves the user's Firebase UID, checks their assigned role, and verifies if they have permission to perform the specified action using the RoleBasedAuthenticationManager. If the user does not have a valid role or is not permitted to perform the action, it throws a ResponseStatusException with an appropriate HTTP status code and error message.
    public void authorizeCurrentUser(ActionDefinition action) {
        String firebaseUid = getAuthenticatedFirebaseUid();
        authorize(firebaseUid, action);
    }

    public void authorize(String firebaseUid, ActionDefinition action) {
        UserAccount user = userService.getFirebaseUid(firebaseUid);

        if (user.getGymRole() == null || user.getGymRole().getRoleType() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have a valid role assigned");
        }

        RoleType roleType = user.getGymRole().getRoleType();
        boolean allowed = roleBasedAuthenticationManager.isPermit(roleType, action);

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Role " + roleType.name() + " is not allowed to perform action " + action.name()
            );
        }
    }
}
