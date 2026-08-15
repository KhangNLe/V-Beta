package app.VBeta.controller;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.application.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ContentReportController {
    private final AuthorizationService authorizationService;
    private final ModerationService moderationService;

    public ContentReportController(AuthorizationService authorizationService,
                                   ModerationService moderationService) {
        this.authorizationService = authorizationService;
        this.moderationService = moderationService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createContentReport(@Valid @RequestBody ReportRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        moderationService.createNewReport(request, firebaseUid);
    }
}
