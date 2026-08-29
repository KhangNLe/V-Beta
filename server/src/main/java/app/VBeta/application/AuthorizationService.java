package app.VBeta.application;

import app.VBeta.application.support.account.RoleBasedAuthenticationManager;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * {@code AuthorizationService} centralizes authentication context access and permission enforcement.
 * It extracts Firebase identity from Spring Security and evaluates requested actions against
 * role-based permissions provided by {@link RoleBasedAuthenticationManager}.
 * <p>
 * Use this service when an endpoint needs to verify whether the current user can perform a
 * domain action described by {@link ActionDefinition}.
 */
@Service
public class AuthorizationService {

    private final RoleBasedAuthenticationManager roleBasedAuthenticationManager;
    private final UserAccountManager userAccountManager;

    /**
     * Constructs a new {@code AuthorizationService} with role and account dependencies.
     *
     * @param roleBasedAuthenticationManager permission evaluator for role/action checks
     * @param userAccountManager account lookup manager for authenticated users
     */
    public AuthorizationService(
            RoleBasedAuthenticationManager roleBasedAuthenticationManager,
            UserAccountManager userAccountManager
    ) {
        this.roleBasedAuthenticationManager = roleBasedAuthenticationManager;
        this.userAccountManager = userAccountManager;
    }

    /**
     * Extracts the authenticated Firebase UID from Spring Security context.
     *
     * @return authenticated Firebase UID
     */
    public String getAuthenticatedFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Missing or invalid authentication token");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new RuntimeException("Missing Firebase UID in authentication token");
        }

        String firebaseUid = String.valueOf(principal);
        if (firebaseUid.isBlank() || "anonymousUser".equals(firebaseUid)) {
            throw new RuntimeException("Missing Firebase UID in authentication token");
        }

        return firebaseUid;
    }

    /**
     * Authorizes the currently authenticated user for a required action.
     *
     * @param action action being requested
     */
    public void authorizeCurrentUser(ActionDefinition action) {
        String firebaseUid = getAuthenticatedFirebaseUid();
        authorize(firebaseUid, action);
    }

    /**
     * Authorizes a user identified by Firebase UID for a required action.
     *
     * @param firebaseUid Firebase UID to authorize
     * @param action action being requested
     */
    public void authorize(String firebaseUid, ActionDefinition action) {
        UserAccount user = userAccountManager.findUserAccountWithRole(firebaseUid);

        if (user == null) {
            throw new RuntimeException("Authenticated user account does not exist");
        }

        if (user.getGymRole() == null || user.getGymRole().getRoleType() == null) {
            throw new RuntimeException("User does not have a valid role assigned");
        }

        RoleType roleType = user.getGymRole().getRoleType();
        boolean allowed = roleBasedAuthenticationManager.isPermit(roleType, action);

        if (!allowed) {
            throw new RuntimeException("Role " + roleType.name() + " is not allowed to perform action " + action.name()
            );
        }
    }

    /**
     * Returns whether {@code user} is permitted to perform {@code action}.
     *
     * @param user account to evaluate
     * @param action action being requested
     * @return {@code true} when the account's role includes the action
     */
    public boolean isPermitted(UserAccount user, ActionDefinition action) {
        if (user == null || user.getGymRole() == null || user.getGymRole().getRoleType() == null) {
            return false;
        }
        return roleBasedAuthenticationManager.isPermit(user.getGymRole().getRoleType(), action);
    }

    public void authorize(UserAccount user,  ActionDefinition action) {
        if (!roleBasedAuthenticationManager.isPermit(user.getGymRole().getRoleType(), action)) {
            throw new RuntimeException("Role " + user.getGymRole().getRoleType() + " is not allowed to perform action ");
        }
    }
}
