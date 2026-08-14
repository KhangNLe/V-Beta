package app.VBeta.controller;

import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.NotificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class EvenNotificationController {
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    public EvenNotificationController(NotificationService notificationService,
                                      AuthorizationService authorizationService) {
        this.notificationService = notificationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/short")
    public ResponseEntity<?> getUserNotifications(){
        try {
            String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
            List<QuickNotificationDTO> response = notificationService.getQuickNotifications(firebaseUid);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch  (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}
