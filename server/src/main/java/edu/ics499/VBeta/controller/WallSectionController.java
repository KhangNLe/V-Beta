package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.api.dto.WallSectionCreationRequest;
import edu.ics499.VBeta.api.dto.WallSectionResponse;
import edu.ics499.VBeta.api.dto.ClimbingProblemDetailResponse;
import edu.ics499.VBeta.application.ClimbingWallService;
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

    public WallSectionController(ClimbingWallService climbingWallService){
        this.climbingWallService = climbingWallService;
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

    @GetMapping("/wall-section/creation")
    @ResponseStatus(HttpStatus.CREATED)
    public WallSectionResponse createWallSection(@Valid @RequestBody WallSectionCreationRequest body){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid authentication");
        }

        String firebaseUid = String.valueOf(auth.getPrincipal());
        // We can use Jay implememtation of the Authentication Service here to check and make sure the action is permitted
        // by the user account with the firebaseUid and ActionDefinition.CREATE_WALL
         return climbingWallService.createNewWallSection(body);
    }

    @GetMapping("wall-section/{wallSectionId}/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteWallSection(@PathVariable Long wallSectionId){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid authentication");
        }
        // Same authentication server for this method

        climbingWallService.deleteWallSection(wallSectionId);
    }

    @GetMapping("/wall-section/{wallSectionId}/reset")
    @ResponseStatus(HttpStatus.OK)
    public void resetWallSection(@PathVariable Long wallSectionId){
        //Same authentication and user action permission here

        climbingWallService.resetWallSection(wallSectionId);
    }
}