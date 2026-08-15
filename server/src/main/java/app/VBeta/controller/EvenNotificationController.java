package app.VBeta.controller;

import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.NotificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.List;

/**
 * {@code EvenNotificationController} exposes in-app notification reads for the
 * authenticated user.
 * <p>
 * Inbox lookups are delegated to {@link NotificationService}. Identity is resolved
 * through {@link AuthorizationService}.
 */
@RestController
@RequestMapping("/api/notification")
public class EvenNotificationController {
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    /**
     * Constructs a new {@code EvenNotificationController} with required services.
     *
     * @param notificationService service for unread inbox reads
     * @param authorizationService service for authentication context
     */
    public EvenNotificationController(NotificationService notificationService,
                                      AuthorizationService authorizationService) {
        this.notificationService = notificationService;
        this.authorizationService = authorizationService;
    }

    /**
     * Returns unread notification summaries for the authenticated user.
     *
     * @return unread notification DTOs
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
}
