package edu.ics499.VBeta.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Role_Permission")
@Getter
@Setter
@AllArgsConstructor
public class RolePermission {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "Gym_Role.role_id")
    private GymRole gymRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", referencedColumnName = "Gym_Action.action_id")
    private GymAction gymAction;
}