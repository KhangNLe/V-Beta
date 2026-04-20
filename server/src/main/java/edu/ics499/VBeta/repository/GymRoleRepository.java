package edu.ics499.VBeta.repository;

import java.util.Optional;

import edu.ics499.VBeta.domain.model.GymRole;
import edu.ics499.VBeta.domain.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link GymRole} entities.
 */
public interface GymRoleRepository extends JpaRepository<GymRole, Long> {
    /**
     * Finds a role row by role type enum.
     *
     * @param roleType role type
     * @return matching role when present
     */
    Optional<GymRole> findByRoleType(RoleType roleType);
}