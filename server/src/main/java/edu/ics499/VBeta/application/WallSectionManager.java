package edu.ics499.VBeta.application;

import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.api.dto.WallSectionRequest;
import edu.ics499.VBeta.api.dto.WallSectionResponse;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.WallSectionRepository;
import edu.ics499.VBeta.repository.ClimbingGradeRepository;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class WallSectionManager {
    private final ClimbingGradeRepository climbingGradeRepository;
    private final ClimbingProblemRepository climbingProblemRepository;
    private final WallSectionRepository wallSectionRepository;

    public WallSectionManager(
            ClimbingProblemRepository climbingProblemRepository,
            ClimbingGradeRepository climbingGradeRepository,
            WallSectionRepository wallSectionRepository
    ){
        this.climbingGradeRepository = climbingGradeRepository;
        this.climbingProblemRepository = climbingProblemRepository;
        this.wallSectionRepository = wallSectionRepository;
    }

    public List<WallSectionResponse> getWallSections(){
        List<WallSection> wallSections = wallSectionRepository.findAll();
        List<WallSectionResponse> wallSectionInfo = new ArrayList<>();

        wallSections.forEach(section -> {
            wallSectionInfo.add(new WallSectionResponse(
                    section.getId(),
                    section.getWallSectionName(),
                    section.getWallInfo()));
        });

        return wallSectionInfo;
    }

    public List<ClimbingProblemResponse> getClimbingProblemByWall(WallSectionRequest section){
        Optional<WallSection> wallSection = wallSectionRepository.findById(section.wallSectionID());

        if (wallSection.isEmpty()){
            throw new IllegalStateException("Wall section: " + section.wallSectionName() + " no longer exist");
        }

        return mapProblemsForWall(wallSection.get());
    }

    public List<ClimbingProblemResponse> getClimbingProblemsByWallSectionId(Long wallSectionId) {
        WallSection wallSection = wallSectionRepository
                .findById(wallSectionId)
                .orElseThrow(() -> new IllegalStateException("Wall section not found: " + wallSectionId));
        return mapProblemsForWall(wallSection);
    }

    private List<ClimbingProblemResponse> mapProblemsForWall(WallSection wallSection) {
        List<ClimbingProblemResponse> problemsInfo = new ArrayList<>();
        List<ClimbingProblem> problems = climbingProblemRepository.findByWallSection(wallSection);
        problems.forEach(problem -> {
            problemsInfo.add(new ClimbingProblemResponse(
                    problem.getId(),
                    problem.getHoldColor(),
                    problem.getProblemInfo(),
                    problem.getClimbingGrade().getGrade()
            ));
        });
        return problemsInfo;
    }
}
