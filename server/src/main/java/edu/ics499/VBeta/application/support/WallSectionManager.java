package edu.ics499.VBeta.application.support;

import edu.ics499.VBeta.api.dto.WallSectionCreationRequest;
import edu.ics499.VBeta.domain.model.WallSection;
import edu.ics499.VBeta.repository.WallSectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

@Service
@Transactional
public class WallSectionManager {
    private final WallSectionRepository wallSectionRepository;

    public WallSectionManager(WallSectionRepository wallSectionRepository){
        this.wallSectionRepository = wallSectionRepository;
    }

    public WallSection findWallSection(Long wallSectionId){
        Optional<WallSection> result = wallSectionRepository.findById(wallSectionId);
        if (result.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Wall Section with id %d does not exist.", wallSectionId));
        }
        return result.get();
    }

    public List<WallSection> getWallSections(){
        return wallSectionRepository.findAll();
    }

    public WallSection createNewWallSection(WallSectionCreationRequest request){
        WallSection section = new WallSection();
        section.setWallInfo(request.wallSectionInfo());
        section.setWallSectionName(request.wallSectionName());
        return wallSectionRepository.save(section);
    }

    public void removeWallSection(Long wallSectionId){
        wallSectionRepository.deleteById(wallSectionId);
    }
}
