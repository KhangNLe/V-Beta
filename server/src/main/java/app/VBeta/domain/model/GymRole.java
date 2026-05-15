package app.VBeta.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code GymRole} stores a role identity used for authorization assignment.
 */
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
