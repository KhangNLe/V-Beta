package app.VBeta.application.support.wall;

import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.application.support.cloud.CloudStorageManager;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.repository.WallSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

/**
 * {@code WallSectionManager} encapsulates persistence and validation rules for
 * {@link WallSection} entities.
 * <p>
 * It serves as the write/read boundary for wall section operations used by higher-level
 * application services.
 */
@Service
@Transactional
public class WallSectionManager {
    private final WallSectionRepository wallSectionRepository;
    private final CloudStorageManager cloudStorageManager;

    /**
     * Constructs a new {@code WallSectionManager} with wall section repository access.
     *
     * @param wallSectionRepository repository for wall section entities
     */
    public WallSectionManager(WallSectionRepository wallSectionRepository,
                              CloudStorageManager cloudStorageManager){
        this.wallSectionRepository = wallSectionRepository;
        this.cloudStorageManager = cloudStorageManager;
    }

    /**
     * Finds a wall section by ID or throws when not found.
     *
     * @param wallSectionId wall section identifier
     * @return matching wall section
     */
    public WallSection findWallSection(Long wallSectionId){
        Optional<WallSection> result = wallSectionRepository.findById(wallSectionId);
        if (result.isEmpty()){
            throw new RuntimeException(String.format("Wall Section with id %d does not exist.", wallSectionId));
        }
        return result.get();
    }

    /**
     * Returns all configured wall sections.
     *
     * @return list of wall sections
     */
    public List<WallSection> getWallSections(){
        return wallSectionRepository.findAll();
    }

    /**
     * Creates and persists a wall section from request data.
     *
     * @param request wall section creation payload
     * @return persisted wall section
     */
    public WallSection createNewWallSection(WallSectionCreationRequest request){
        WallSection section = new WallSection();
        section.setWallInfo(request.wallSectionInfo());
        section.setWallSectionName(request.wallSectionName());
        section.setImageObjectName(request.objectFileName());
        section.setWallImageUrl(section.getWallImageUrl());
        return wallSectionRepository.save(section);
    }

    /**
     * Removes a wall section by identifier.
     *
     * @param wallSectionId wall section identifier
     */
    public void removeWallSection(Long wallSectionId){
        WallSection wall = findWallSection(wallSectionId);
        cloudStorageManager.deleteImageObject(wall.getImageObjectName());
        wallSectionRepository.deleteById(wallSectionId);
    }
}
