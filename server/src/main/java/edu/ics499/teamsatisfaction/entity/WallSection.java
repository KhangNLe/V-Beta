package edu.ics499.teamsatisfaction.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "`Wall_Section`")
public class WallSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer wall_section_id;

    @Column(nullable = false)
    private String wall_section_name;

    /** Short description / details for the wall area (column {@code info}). Use length mapping instead of @Lob for plain VARCHAR/TEXT in existing schemas. */
    @Column(name = "info", nullable = true, length = 65_535)
    private String info;

    @JsonProperty("wall_section_id")
    public Integer getWall_section_id() {
        return wall_section_id;
    }

    public void setWall_section_id(Integer wall_section_id) {
        this.wall_section_id = wall_section_id;
    }

    @JsonProperty("wall_section_name")
    public String getWall_section_name() {
        return wall_section_name;
    }

    public void setWall_section_name(String wall_section_name) {
        this.wall_section_name = wall_section_name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
