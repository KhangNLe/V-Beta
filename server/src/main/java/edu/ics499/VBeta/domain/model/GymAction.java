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
    private String actionDefinition;
}