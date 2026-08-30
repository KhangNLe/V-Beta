package app.VBeta.mvc;

import app.VBeta.api.dto.notification.EventTypeDTO;
import app.VBeta.api.dto.notification.NotificationClickDTO;
import app.VBeta.api.dto.notification.NotificationClickKind;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        1L,
                        new EventTypeDTO("REPORT_CREATED", "A user submitted a content report"),
                        new NotificationClickDTO(
                                NotificationClickKind.REPORT_QUEUE,
                                1L,null, null, null, null
                        ),
                        Instant.now()
                )
        ));

        mockMvc.perform(get("/api/notification/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].summary.eventTypeName").value("REPORT_CREATED"))
                .andExpect(jsonPath("$[0].summary.description").value("A user submitted a content report"));

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

    @Test
    @DisplayName("PATCH /api/notification/short?notificationId= return 200 after mark read")
    void returns200_whenReadNotificationShort() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");

        mockMvc.perform(patch("/api/notification/short").param("notificationId", "1"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).updateNotificationToRead("testFirebaseUid", 1L);
    }

    @Test
    @DisplayName("PATH /api/nottificaion/short?notificationId= return 400 without request param")
    void returns400_whenReadNotificationShort() throws Exception {
        mockMvc.perform(patch("/api/notification/short"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notificationService);
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("PATH /api/notification/short?notificationId= return 404 without authentication token")
    void returns404_whenReadNotificationShortWithoutAuthenticationToken() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        mockMvc.perform(patch("/api/notification/short").param("notificationId", "1"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("PATCH /api/notification/short maps missing notification to 404")
    void returns404_whenMarkReadNotificationIsMissing() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationService)
                .updateNotificationToRead("testFirebaseUid", 99L);

        mockMvc.perform(patch("/api/notification/short").param("notificationId", "99"))
                .andExpect(status().isNotFound());

        verify(notificationService, times(1)).updateNotificationToRead("testFirebaseUid", 99L);
    }

    @Test
    @DisplayName("GET /api/notification/all returns paged inbox with default offset 1")
    void returns200_whenGetAllNotificationsUsesDefaultOffset() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("adminUid");
        when(notificationService.getAllQuickNotifications("adminUid", 1)).thenReturn(List.of(
                sampleQuickNotification(
                        81L,
                        "REPORT_CREATED",
                        "A user submitted a content report",
                        11L
                )
        ));

        mockMvc.perform(get("/api/notification/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].notificationId").value(81))
                .andExpect(jsonPath("$[0].summary.eventTypeName").value("REPORT_CREATED"))
                .andExpect(jsonPath("$[0].click.kind").value("REPORT_QUEUE"))
                .andExpect(jsonPath("$[0].click.reportId").value(11));

        verify(notificationService, times(1)).getAllQuickNotifications("adminUid", 1);
    }

    @Test
    @DisplayName("GET /api/notification/all?offset=2 forwards the page offset")
    void returns200_whenGetAllNotificationsUsesExplicitOffset() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("climberUid");
        when(notificationService.getAllQuickNotifications("climberUid", 2)).thenReturn(List.of());

        mockMvc.perform(get("/api/notification/all").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(notificationService, times(1)).getAllQuickNotifications("climberUid", 2);
    }

    @Test
    @DisplayName("GET /api/notification/all returns mixed report and appeal event types")
    void returns200_whenGetAllNotificationsIncludesReportAndAppealTypes() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("adminUid");
        when(notificationService.getAllQuickNotifications("adminUid", 1)).thenReturn(List.of(
                sampleQuickNotification(
                        81L,
                        "REPORT_CREATED",
                        "A user submitted a content report",
                        11L
                ),
                sampleQuickNotification(
                        83L,
                        "CONTENT_REMOVED",
                        "One of your content had been reported and removed.",
                        11L
                )
        ));

        mockMvc.perform(get("/api/notification/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].summary.eventTypeName").value("REPORT_CREATED"))
                .andExpect(jsonPath("$[1].summary.eventTypeName").value("CONTENT_REMOVED"))
                .andExpect(jsonPath("$[1].click.kind").value("REPORT_QUEUE"));
    }

    @Test
    @DisplayName("GET /api/notification/all returns empty list when inbox is empty")
    void returns200_withEmptyList_whenGetAllNotificationsHasNoRows() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("climberUid");
        when(notificationService.getAllQuickNotifications("climberUid", 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/notification/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/notification/all maps missing account to 404")
    void returns404_whenGetAllNotificationsUserIsMissing() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("missingUid");
        when(notificationService.getAllQuickNotifications("missingUid", 1))
                .thenThrow(new RuntimeException("User is not found"));

        mockMvc.perform(get("/api/notification/all"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/notification/all maps auth failure to 404")
    void returns404_whenGetAllNotificationsAuthenticationFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        mockMvc.perform(get("/api/notification/all"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notification/all returns 400 for a non-numeric offset")
    void returns400_whenGetAllNotificationsOffsetIsInvalid() throws Exception {
        mockMvc.perform(get("/api/notification/all").param("offset", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
        verifyNoInteractions(authorizationService);
    }

    private static QuickNotificationDTO sampleQuickNotification(
            Long notificationId,
            String eventTypeName,
            String description,
            Long reportId
    ) {
        return new QuickNotificationDTO(
                notificationId,
                new EventTypeDTO(eventTypeName, description),
                new NotificationClickDTO(
                        NotificationClickKind.REPORT_QUEUE,
                        reportId, null, null, null, null
                ),
                Instant.parse("2026-08-14T19:11:00Z")
        );
    }
}
