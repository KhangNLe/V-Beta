package app.VBeta.controller;

import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.image.ImageStorageRequest;
import app.VBeta.api.dto.image.ProfileImageCreationRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ImageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social")
public class SocialMediaController {
    private final AuthorizationService authorizationService;
    private final ImageService imageService;

    public SocialMediaController(AuthorizationService authorizationService,
                                 ImageService imageService) {
        this.authorizationService = authorizationService;
        this.imageService = imageService;
    }

    @GetMapping("/image/signed-url")
    public ResponseEntity<?> getImageSignedURL(@Valid @ModelAttribute ImageStorageRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            CloudFileStorageResponse response = imageService.createSignedUrl(firebaseUid, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/image/upload")
    public ResponseEntity<?> uploadImage(@Valid @ModelAttribute ProfileImageCreationRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            imageService.saveImage(firebaseUid, request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/image/problem")
    public ResponseEntity<?> removeProblemImage(@RequestParam Long climbingProblemId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            imageService.problemImageDeletion(firebaseUid, climbingProblemId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return  new  ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/image/wall")
    public ResponseEntity<?> removeWallImage(@RequestParam Long wallSectionId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            imageService.wallSectionImageDeletion(firebaseUid, wallSectionId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return  new  ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
