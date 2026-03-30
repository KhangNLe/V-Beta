package edu.ics499.teamsatisfaction.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ics499.teamsatisfaction.entity.WallSection;
import edu.ics499.teamsatisfaction.repository.WallSectionRepository;

@RestController
public class WallSectionController {

    private final WallSectionRepository repository;

    public WallSectionController(WallSectionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/wall-sections")
    public List<WallSection> getWallSections() {
        return repository.findAll();
    }
}
