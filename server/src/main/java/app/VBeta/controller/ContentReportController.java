package app.VBeta.controller;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.api.dto.report.ReportsPayload;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * {@code ContentReportController} accepts authenticated content reports.
 * <p>
 * Report creation is delegated to {@link ModerationService}. The reporter identity
 * is taken from {@link AuthorizationService}, not from the request body.
 */
@RestController
@RequestMapping("/api/report")
public class ContentReportController {
    private final AuthorizationService authorizationService;
    private final ModerationService moderationService;

    /**
     * Constructs a new {@code ContentReportController} with required services.
     *
     * @param authorizationService service for authentication context
     * @param moderationService service for report creation and admin notification
     */
    public ContentReportController(AuthorizationService authorizationService,
                                   ModerationService moderationService) {
        this.authorizationService = authorizationService;
        this.moderationService = moderationService;
    }

    /**
     * Creates a new content report for the authenticated user.
     * <p>
     * On success the response is {@code 201} with an empty body.
     *
     * @param request report target, category, and reason payload
     */
    @PostMapping("/create")
    public ResponseEntity<?> createContentReport(@Valid @RequestBody ReportRequest request){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            moderationService.createNewReport(request, firebaseUid);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(@RequestParam(required = false) Long reportId){
        try{
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            ReportsPayload reports;
            if (reportId == null){
                reports = moderationService.getReportQueue(firebaseUid);
            } else {
                reports = moderationService.getReport(firebaseUid, reportId);
            }
            return new ResponseEntity<>(reports, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
