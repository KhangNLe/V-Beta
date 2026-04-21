package edu.ics499.VBeta.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code WallSection} represents a named segment of the climbing wall.
 */
@Entity
@Table(name = "Wall_Section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WallSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wall_section_id")
    private Long id;

    @Column(name = "info", length = 250)
    private String wallInfo;

    @Column(name = "wall_section_name", nullable = false, length = 30)
    private String wallSectionName;
}
