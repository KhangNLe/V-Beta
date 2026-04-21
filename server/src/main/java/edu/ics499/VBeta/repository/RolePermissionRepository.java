package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.RolePermission;
import edu.ics499.VBeta.domain.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for {@link RolePermission} mappings.
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    /**
     * Returns all role-permission mappings with role and action eagerly fetched.
     *
     * @return role-permission mappings with linked role/action data
     */
    @Query("SELECT DISTINCT rp FROM RolePermission rp "
            + "JOIN FETCH rp.gymRole JOIN FETCH rp.gymAction")
    List<RolePermission> findAllFetchingRoleAndAction();
}
