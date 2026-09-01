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

/**
 * REST controller for image upload and deletion flows backed by Google Cloud Storage.
 * <p>
 * Clients follow the same signed-PUT pattern as solution-beta videos:
 * request a signed URL, upload bytes directly to GCS, then persist metadata through this API.
 * Wall-section and problem images are action-gated; profile images require the caller to match
 * {@code userId} on the request.
 */
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

    /**
     * Generates a signed GCS PUT URL for an image upload.
     *
     * @param request upload target and file metadata bound from query parameters
     * @return signed upload details and resulting public URL, or an error status
     */
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

    /**
     * Persists uploaded image metadata after the client completes the GCS PUT.
     *
     * @param request target entity, object key, and public URL bound from query parameters
     * @return {@code 200} when metadata is saved
     */
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

    /**
     * Deletes a climbing problem image from storage and clears persisted metadata.
     *
     * @param climbingProblemId active problem identifier
     * @return {@code 200} when deletion succeeds
     */
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

    /**
     * Deletes a wall section image from storage and clears persisted metadata.
     *
     * @param wallSectionId wall section identifier
     * @return {@code 200} when deletion succeeds
     */
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
