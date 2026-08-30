package app.VBeta.controller;

import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.NotificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.List;

/**
 * {@code EvenNotificationController} exposes the authenticated user's in-app inbox.
 * <p>
 * {@code GET /short} returns unread rows with click metadata.
 * {@code GET /all} returns a page of the caller's read and unread rows
 * (newest first, 10 per page).
 * {@code PATCH /short?notificationId=} marks one of the caller's rows read.
 * Identity is resolved through {@link AuthorizationService}. There is no action gate.
 */
@RestController
@RequestMapping("/api/notification")
public class EvenNotificationController {
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    /**
     * Constructs a new {@code EvenNotificationController} with required services.
     *
     * @param notificationService service for unread inbox, paged all-inbox, and mark-read
     * @param authorizationService service for authentication context
     */
    public EvenNotificationController(NotificationService notificationService,
                                      AuthorizationService authorizationService) {
        this.notificationService = notificationService;
        this.authorizationService = authorizationService;
    }

    /**
     * Returns unread inbox items for the authenticated user.
     * <p>
     * On success the response is {@code 200} with an array of
     * {@link QuickNotificationDTO}. {@code RuntimeException} is mapped to
     * {@code 401} (missing auth or missing account).
     *
     * @return unread notification DTOs (empty list when the inbox is empty)
     */
    @GetMapping("/short")
    public ResponseEntity<?> getUserNotifications(){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            List<QuickNotificationDTO> response = notificationService.getQuickNotifications(firebaseUid);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch  (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Marks one of the authenticated user's notifications as read.
     * <p>
     * On success the response is {@code 200} with an empty body, including when
     * the row was already read. {@code RuntimeException} is mapped to {@code 404}
     * (missing auth, missing account, unknown id, or another user's notification).
     * Missing {@code notificationId} is {@code 400} from Spring.
     *
     * @param notificationId inbox row owned by the caller
     * @return empty {@code 200} on success
     */
    @PatchMapping("/short")
    public ResponseEntity<?> updateNotificationToRead(@RequestParam() Long notificationId){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            notificationService.updateNotificationToRead(firebaseUid, notificationId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns one page of the authenticated user's inbox (read and unread).
     * <p>
     * Rows are newest-first. Page size is 10. {@code offset} is 1-based
     * (default {@code 1}): page 1 is rows 1–10, page 2 is 11–20, and so on.
     * The payload is the same {@link QuickNotificationDTO} shape as
     * {@code GET /short} and does not include {@code readAt}.
     * On success the response is {@code 200} with an array (empty when the
     * page has no rows). {@code RuntimeException} is mapped to {@code 404}
     * (missing auth or missing account). Non-numeric {@code offset} is
     * {@code 400} from Spring.
     *
     * @param offset 1-based page number (default {@code 1})
     * @return paged notification DTOs for the caller
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllQuickNotifications(@RequestParam(defaultValue = "1") Integer offset){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            return new  ResponseEntity<>(
                    notificationService.getAllQuickNotifications(firebaseUid, offset), HttpStatus.OK
            );
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
