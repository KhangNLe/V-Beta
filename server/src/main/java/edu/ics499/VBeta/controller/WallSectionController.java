package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.*;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ClimbingWallService;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/home")
public class WallSectionController {
    private final ClimbingWallService climbingWallService;
    private final AuthorizationService authorizationService;

    public WallSectionController(ClimbingWallService climbingWallService,
                                 AuthorizationService authorizationService){
        this.climbingWallService = climbingWallService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/wall-sections")
    @Transactional(readOnly = true)
    public List<WallSectionResponse> wallSections() {
        return climbingWallService.getWallSections();
    }

    @GetMapping("/wall-sections/{wallSectionId}/problems")
    @Transactional(readOnly = true)
    public List<ClimbingProblemResponse> problemsForWallSection(@PathVariable Long wallSectionId) {
        return climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
    }

    @GetMapping("wall-sections/{wallSectionId}/problems/{problemID}")
    @Transactional(readOnly = true)
    public ClimbingProblemDetailResponse getProblemDetail(@PathVariable Long wallSectionId, @PathVariable Long problemID){
        return climbingWallService.getClimbingProblem(problemID);
    }

    @GetMapping("/wall-sections/creation")
    @ResponseStatus(HttpStatus.CREATED)
    public WallSectionResponse createWallSection(@Valid @RequestBody WallSectionCreationRequest body){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_WALL);

         return climbingWallService.createNewWallSection(body);
    }

    @GetMapping("wall-sections/{wallSectionId}/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteWallSection(@PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_WALL);

        climbingWallService.deleteWallSection(wallSectionId);
    }

    @GetMapping("/wall-sections/{wallSectionId}/reset")
    @ResponseStatus(HttpStatus.OK)
    public void resetWallSection(@PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.RESET_WALL);

        climbingWallService.resetWallSection(wallSectionId);
    }

    @GetMapping("/wall-sections/{wallSectionId}/problems/create")
    public ClimbingProblemResponse createClimbingProblem(@Valid @RequestBody ClimbingProblemCreationRequest request,
                                                         @PathVariable Long wallSectionId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.CREATE_PROBLEM);

        return climbingWallService.createNewClimbingProblem(wallSectionId, request);
    }

    @GetMapping("/wall-sections/{wallSectionId}/problems/{problemId}/delete")
    public List<ClimbingProblemResponse> deleteClimbingProblem(@PathVariable Long wallSectionId,
                                                               @PathVariable Long problemId){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        authorizationService.authorize(firebaseUid, ActionDefinition.DELETE_PROBLEM);

        climbingWallService.deleteClimbingProblem(problemId);
        return climbingWallService.getClimbingProblemsByWallSectionId(wallSectionId);
    }


}