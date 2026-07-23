package app.VBeta.repository;

import java.util.Optional;

import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
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