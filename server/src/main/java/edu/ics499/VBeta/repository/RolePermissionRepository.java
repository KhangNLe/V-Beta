package edu.ics499.VBeta.repository;

import edu.ics499.VBeta.domain.model.RolePermission;
import edu.ics499.VBeta.domain.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("SELECT DISTINCT rp FROM RolePermission rp "
            + "JOIN FETCH rp.gymRole JOIN FETCH rp.gymAction")
    List<RolePermission> findAllFetchingRoleAndAction();
}
