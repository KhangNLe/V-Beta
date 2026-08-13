package app.VBeta.controller;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.application.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class ContentReportController {
    private final AuthorizationService authorizationService;
    private final ModerationService moderationService;
    private final NotificationService notificationService;

    public ContentReportController(AuthorizationService authorizationService,
                                   ModerationService moderationService,
                                   NotificationService notificationService) {
        this.authorizationService = authorizationService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createContentReport(@Valid @RequestBody ReportRequest request){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        moderationService.createNewReport(request, firebaseUid);
    }
}
