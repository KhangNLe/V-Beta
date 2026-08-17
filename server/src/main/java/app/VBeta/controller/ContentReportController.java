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
 * {@code ContentReportController} accepts authenticated content reports and
 * exposes the admin report queue and detail.
 * <p>
 * Create-report is authenticated only. Queue and detail require
 * {@link app.VBeta.domain.model.actions.ActionDefinition#VIEW_REPORTS}.
 * Caller identity is taken from {@link AuthorizationService}, not the request body.
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
     * @param moderationService service for report creation, queue, and detail
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

    /**
     * Returns ranked OPEN report cases for an admin.
     * <p>
     * With no {@code reportId}, the response is the full queue. With {@code reportId},
     * the response is the OPEN case for that report's target (all OPEN reporters on
     * the same discussion/problem/wall/user). Cases the viewer owns are omitted.
     * Ranking uses {@code queueScore = Σ (category weight × report count)}.
     * <p>
     * On success the response is {@code 200} with a {@link ReportsPayload}.
     * {@code RuntimeException} is mapped to {@code 404} (including missing reports
     * and missing {@code VIEW_REPORTS} permission).
     *
     * @param reportId optional report id for a single-case detail lookup
     * @return ranked queue or one case; empty {@code reports} when nothing is visible
     */
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
