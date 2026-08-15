package app.VBeta.controller;

import app.VBeta.api.dto.problems.ClimbingProblemCreationRequest;
import app.VBeta.api.dto.problems.ClimbingProblemDetailResponse;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.api.dto.walls.WallSectionResponse;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.repository.ClimbingProblemRepository;
import jakarta.validation.Valid;
import org.aspectj.apache.bcel.classfile.annotation.RuntimeTypeAnnos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/home")
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
    public ResponseEntity<?> wallSections() {
        try {
            List<WallSectionResponse> response = climbingWallService.getWallSections();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns active climbing problems for a wall section.
     *
     * @param wallSectionId wall section identifier
     * @return list of climbing problem summaries
     */
    @GetMapping("/wall-sections/{wallSectionId}/problems")
    @Transactional(readOnly = true)
    public ResponseEntity<?> problemsForWallSection(@PathVariable Long wallSectionId) {
        try {
            List<ClimbingProblemResponse> responses = climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
            return new ResponseEntity<>(responses, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    public ResponseEntity<?> getProblemDetail(@PathVariable Long wallSectionId, @PathVariable Long problemID){
        try {
            ClimbingProblemDetailResponse response = climbingWallService.getClimbingProblem(problemID);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates a new wall section.
     *
     * @param body wall section creation payload
     * @return created wall section response
     */
    @PostMapping("/wall-section/creation")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createWallSection(@Valid @RequestBody WallSectionCreationRequest body){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_WALL);

            WallSectionResponse response =  climbingWallService.createNewWallSection(body);

            return new  ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a wall section and related climbing problem data.
     *
     * @param wallSectionId wall section identifier
     */
    @DeleteMapping("wall-section/{wallSectionId}/delete")
    public ResponseEntity<?> deleteWallSection(@PathVariable Long wallSectionId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_WALL);

            climbingWallService.deleteWallSection(wallSectionId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Archives all active problems in a wall section.
     *
     * @param wallSectionId wall section identifier
     */
    @PatchMapping("/wall-section/{wallSectionId}/reset")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> resetWallSection(@PathVariable Long wallSectionId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.RESET_WALL);
            climbingWallService.resetWallSection(wallSectionId);

            return new ResponseEntity<>( HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates a new climbing problem under a wall section.
     *
     * @param request climbing problem creation payload
     * @param wallSectionId wall section identifier
     * @return created climbing problem response
     */
    @PostMapping("/wall-sections/{wallSectionId}/problems/create")
    public ResponseEntity<?> createClimbingProblem(@Valid @RequestBody ClimbingProblemCreationRequest request,
                                                         @PathVariable Long wallSectionId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_PROBLEM);

            ClimbingProblemResponse response =  climbingWallService.createNewClimbingProblem(wallSectionId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a climbing problem and returns remaining active problems in the wall section.
     *
     * @param wallSectionId wall section identifier
     * @param problemId climbing problem identifier
     * @return updated list of climbing problem summaries
     */
    @PatchMapping("/wall-sections/{wallSectionId}/problems/{problemId}/delete")
    public ResponseEntity<?> deleteClimbingProblem(@PathVariable Long wallSectionId,
                                                               @PathVariable Long problemId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_PROBLEM);

            climbingWallService.deleteClimbingProblem(problemId);
            List<ClimbingProblemResponse> response =  climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e){
            return  new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}