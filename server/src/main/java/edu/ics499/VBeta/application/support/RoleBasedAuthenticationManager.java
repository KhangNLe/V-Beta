package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class RoleBasedAuthenticationManager {
    private Map<RoleType, Set<ActionDefinition>> roleBasedPermission;
    private final RolePermissionRepository rolePermissionRepository;

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

    public boolean isPermit(RoleType role, ActionDefinition action){
        Set<ActionDefinition> actions = roleBasedPermission.getOrDefault(role, null);
        return (actions != null && actions.contains(action));
    }
}
