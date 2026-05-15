package app.VBeta.controller;

import app.VBeta.api.dto.*;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code WallSectionController} exposes wall and climbing problem endpoints under {@code /home}.
 * <p>
 * It delegates wall/problem operations to {@link ClimbingWallService} and applies action-based
 * authorization checks through {@link AuthorizationService}.
 */
@RestController
@RequestMapping("/home")
public class WallSectionController {
    private final ClimbingWallService climbingWallService;
    private final AuthorizationService authorizationService;

    /**
     * Constructs a new {@code WallSectionController} with service dependencies.
     *
     * @param climbingWallService service for wall and problem operations
     * @param authorizationService service for user authorization checks
     */
    public WallSectionController(ClimbingWallService climbingWallService,
                                 AuthorizationService authorizationService){
        this.climbingWallService = climbingWallService;
        this.authorizationService = authorizationService;
    }

    /**
     * Returns all wall sections.
     *
     * @return list of wall section response objects
     */
    @GetMapping("/wall-sections")
    @Transactional(readOnly = true)
    public List<WallSectionResponse> wallSections() {
        return climbingWallService.getWallSections();
    }

    /**
     * Returns active climbing problems for a wall section.
     *
     * @param wallSectionId wall section identifier
     * @return list of climbing problem summaries
     */
    @GetMapping("/wall-sections/{wallSectionId}/problems")
    @Transactional(readOnly = true)
    public List<ClimbingProblemResponse> problemsForWallSection(@PathVariable Long wallSectionId) {
        return climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
    }

    /**
     * Returns full problem details for a wall section/problem pair.
     *
     * @param wallSectionId wall section identifier
     * @param problemID climbing problem identifier
     * @return detailed climbing problem response
     */
    @GetMapping("wall-sections/{wallSectionId}/problems/{problemID}")
    @Transactional(readOnly = true)
    public ClimbingProblemDetailResponse getProblemDetail(@PathVariable Long wallSectionId, @PathVariable Long problemID){
        return climbingWallService.getClimbingProblem(problemID);
    }

    /**
     * Creates a new wall section.
     *
     * @param body wall section creation payload
     * @return created wall section response
     */
    @PostMapping("/wall-section/creation")
    @ResponseStatus(HttpStatus.CREATED)
    public WallSectionResponse createWallSection(@Valid @RequestBody WallSectionCreationRequest body){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_WALL);

         return climbingWallService.createNewWallSection(body);
    }

    /**
     * Deletes a wall section and related climbing problem data.
     *
     * @param wallSectionId wall section identifier
     */
    @DeleteMapping("wall-section/{wallSectionId}/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteWallSection(@PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_WALL);

        climbingWallService.deleteWallSection(wallSectionId);
    }

    /**
     * Archives all active problems in a wall section.
     *
     * @param wallSectionId wall section identifier
     */
    @PostMapping("/wall-section/{wallSectionId}/reset")
    @ResponseStatus(HttpStatus.OK)
    public void resetWallSection(@PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.RESET_WALL);

        climbingWallService.resetWallSection(wallSectionId);
    }

    /**
     * Creates a new climbing problem under a wall section.
     *
     * @param request climbing problem creation payload
     * @param wallSectionId wall section identifier
     * @return created climbing problem response
     */
    @PostMapping("/wall-sections/{wallSectionId}/problems/create")
    public ClimbingProblemResponse createClimbingProblem(@Valid @RequestBody ClimbingProblemCreationRequest request,
                                                         @PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_PROBLEM);

        return climbingWallService.createNewClimbingProblem(wallSectionId, request);
    }

    /**
     * Deletes a climbing problem and returns remaining active problems in the wall section.
     *
     * @param wallSectionId wall section identifier
     * @param problemId climbing problem identifier
     * @return updated list of climbing problem summaries
     */
    @DeleteMapping("/wall-sections/{wallSectionId}/problems/{problemId}/delete")
    public List<ClimbingProblemResponse> deleteClimbingProblem(@PathVariable Long wallSectionId,
                                                               @PathVariable Long problemId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_PROBLEM);

        climbingWallService.deleteClimbingProblem(problemId);
        return climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
    }
}