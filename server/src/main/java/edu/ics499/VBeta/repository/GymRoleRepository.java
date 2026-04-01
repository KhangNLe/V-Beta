package edu.ics499.VBeta.repository;

import java.util.Optional;

import edu.ics499.VBeta.domain.model.GymRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRoleRepository extends JpaRepository<GymRole, Long> {
    Optional<GymRole> findByRoleType(String roleType);
}