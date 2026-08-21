package app.VBeta.controller;

import app.VBeta.api.dto.moderation.ModerationPayload;
import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * {@code ModerationController} accepts admin report-queue decisions and exposes
 * the append-only moderation logbook.
 * <p>
 * Resolve is action-gated with
 * {@link app.VBeta.domain.model.actions.ActionDefinition#MODERATE_REPORT}.
 * Logbook reads are action-gated with
 * {@link app.VBeta.domain.model.actions.ActionDefinition#VIEW_MODERATION_LOGS}.
 * Caller identity is taken from {@link AuthorizationService}, not the request body.
 */
@RestController
@RequestMapping("/api/moderate")
public class ModerationController {
    private final AuthorizationService authorizationService;
    private final ModerationService moderationService;

    /**
     * Constructs a new {@code ModerationController} with required services.
     *
     * @param authorizationService service for authentication context
     * @param moderationService service for report-queue resolve and logbook reads
     */
    public ModerationController(AuthorizationService authorizationService,
                                ModerationService moderationService) {
        this.authorizationService = authorizationService;
        this.moderationService = moderationService;
    }

    /**
     * Applies a dismiss or content-removed decision to each eligible report id.
     * <p>
     * On success the response is {@code 200} with an empty body, including when
     * every id was skipped. {@code RuntimeException} is mapped to {@code 404}
     * (missing account, missing {@code MODERATE_REPORT}, or unsupported appeal
     * decision). Bean validation failures still return {@code 400}.
     *
     * @param moderationRequest report ids, decision, and required admin notes
     * @return empty {@code 200} on success
     */
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

    /**
     * Returns moderation logbook rows for an admin.
     * <p>
     * With {@code moderationId}, the response is that one row. Without it, the
     * response is a page of 25 rows newest-first ({@code offSetPlace} is 1-based;
     * page {@code n} skips {@code 25 × (n - 1)} rows). {@code offSetPlace <= 0}
     * is {@code 400}. {@code RuntimeException} is mapped to {@code 404}
     * (missing account, missing {@code VIEW_MODERATION_LOGS}, or unknown id).
     *
     * @param moderationId optional logbook row id
     * @param offSetPlace 1-based page when listing (default {@code 1})
     * @return {@link ModerationPayload}; empty {@code moderationLogs} when the page has no rows
     */
    @GetMapping("/logbook")
    public ResponseEntity<?> getLogbook(@RequestParam(required = false) Long moderationId,
                                        @RequestParam(defaultValue = "1") int offSetPlace) {
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            if (offSetPlace <= 0){
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            ModerationPayload payload;
            if (moderationId != null) {
                payload = moderationService.getModerationLog(firebaseUid, moderationId);
            } else {
                payload = moderationService.getLogbook(firebaseUid, offSetPlace);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (RuntimeException e) {
            return  new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
