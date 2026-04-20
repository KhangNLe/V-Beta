package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * {@code RoleBasedAuthenticationManager} loads and caches allowed actions per role.
 * <p>
 * Permission mappings are built from persisted {@link RolePermission} records and used
 * to evaluate runtime authorization checks for {@link ActionDefinition}.
 */
@Service
@Transactional
public class RoleBasedAuthenticationManager {
    private Map<RoleType, Set<ActionDefinition>> roleBasedPermission;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Constructs a new {@code RoleBasedAuthenticationManager} and initializes permission cache.
     *
     * @param rolePermissionRepository repository providing role/action mappings
     */
    public RoleBasedAuthenticationManager(RolePermissionRepository rolePermissionRepository){
        this.rolePermissionRepository = rolePermissionRepository;
        initiateRoleBasePermission();
    }

    private void initiateRoleBasePermission(){
        roleBasedPermission = new HashMap<>();
        List<RolePermission> permissions = rolePermissionRepository.findAllFetchingRoleAndAction();
        permissions.forEach(permission ->{
            Set<ActionDefinition> actions = roleBasedPermission.getOrDefault(
                    permission.getGymRole().getRoleType(),
                    new HashSet<>()
            );
            actions.add(permission.getGymAction().getActionDefinition());
            roleBasedPermission.put(permission.getGymRole().getRoleType(), actions);
        });
    }

    /**
     * Checks whether a role is permitted to execute an action.
     *
     * @param role role type to evaluate
     * @param action action being requested
     * @return true when role includes the requested action
     */
    public boolean isPermit(RoleType role, ActionDefinition action){
        Set<ActionDefinition> actions = roleBasedPermission.getOrDefault(role, null);
        return (actions != null && actions.contains(action));
    }
}
