package edu.ics499.teamsatisfaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ics499.teamsatisfaction.entity.WallSection;

public interface WallSectionRepository extends JpaRepository<WallSection, Integer> {
}
