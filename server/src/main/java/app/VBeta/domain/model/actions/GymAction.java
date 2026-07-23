package app.VBeta.domain.model.actions;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code GymAction} stores a single action that can be granted to roles.
 */
@Entity
@Table(name = "Gym_Action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "action_definition", length = 50)
    @Enumerated(value = EnumType.STRING)
    private ActionDefinition actionDefinition;
}