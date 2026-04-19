package edu.ics499.VBeta.application;

import edu.ics499.VBeta.application.support.RoleBasedAuthenticationManager;
import edu.ics499.VBeta.application.support.UserAccountManager;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.domain.model.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {

    private final RoleBasedAuthenticationManager roleBasedAuthenticationManager;
    private final UserAccountManager userAccountManager;

    public AuthorizationService(
            RoleBasedAuthenticationManager roleBasedAuthenticationManager,
            UserAccountManager userAccountManager
    ) {
        this.roleBasedAuthenticationManager = roleBasedAuthenticationManager;
        this.userAccountManager = userAccountManager;
    }

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

    public void authorizeCurrentUser(ActionDefinition action) {
        String firebaseUid = getAuthenticatedFirebaseUid();
        authorize(firebaseUid, action);
    }

    public void authorize(String firebaseUid, ActionDefinition action) {
        UserAccount user = userAccountManager.findUserAccountWithRole(firebaseUid);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user account does not exist");
        }

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
