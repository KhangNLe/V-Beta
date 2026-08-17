package app.VBeta.mvc;

import app.VBeta.api.dto.notification.EventTypeDTO;
import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.NotificationService;
import app.VBeta.controller.EvenNotificationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EvenNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class EvenNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("GET /api/notification/short returns unread inbox items")
    void returns200_whenAuthenticatedAdminHasUnreadNotifications() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("adminUid");
        when(notificationService.getQuickNotifications("adminUid")).thenReturn(List.of(
                new QuickNotificationDTO(
                        new EventTypeDTO("REPORT_CREATED", "A user submitted a content report"),
                        LocalDateTime.of(2026, 8, 14, 19, 11)
                )
        ));

        mockMvc.perform(get("/api/notification/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event.eventTypeName").value("REPORT_CREATED"))
                .andExpect(jsonPath("$[0].event.description").value("A user submitted a content report"));

        verify(notificationService, times(1)).getQuickNotifications("adminUid");
    }

    @Test
    @DisplayName("GET /api/notification/short returns empty list when inbox is empty")
    void returns200_withEmptyList_whenNoUnreadNotifications() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("climberUid");
        when(notificationService.getQuickNotifications("climberUid")).thenReturn(List.of());

        mockMvc.perform(get("/api/notification/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/notification/short maps auth failure to 401")
    void returns401_whenAuthenticationFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        mockMvc.perform(get("/api/notification/short"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }
}
