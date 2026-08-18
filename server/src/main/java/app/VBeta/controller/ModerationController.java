package app.VBeta.controller;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.application.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderate")
public class ModerationController {
    private final AuthorizationService authorizationService;
    private final ModerationService moderationService;

    public ModerationController(AuthorizationService authorizationService,
                                ModerationService moderationService) {
        this.authorizationService = authorizationService;
        this.moderationService = moderationService;
    }

    @PostMapping("/report")
    public ResponseEntity<?> moderateReport(@Valid @RequestBody ModerationRequest moderationRequest) {
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            moderationService.createModerationForReportQueue(moderationRequest, firebaseUid);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
