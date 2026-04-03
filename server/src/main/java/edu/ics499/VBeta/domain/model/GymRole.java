package edu.ics499.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Gym_Role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_type", nullable = false, length = 25)
    @Enumerated(value = EnumType.STRING)
    private RoleType roleType;
}
