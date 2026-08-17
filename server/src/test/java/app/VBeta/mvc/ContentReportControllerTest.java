package app.VBeta.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.report.*;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.controller.ContentReportController;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.report.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = ContentReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ContentReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    AuthorizationService authorizationService;

    @MockitoBean
    ModerationService moderationService;

    @Test
    @DisplayName("Test for user report discussion. Happy path return 201")
    void happyPath_return201_whenAuthenticatedUserReportDiscussion() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");

        doNothing().when(moderationService).createNewReport(
                org.mockito.ArgumentMatchers.any(ReportRequest.class),
                org.mockito.ArgumentMatchers.eq("testFirebaseUid")
        );

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy comment",
                ReportCategoryName.SPAM,
                2L
        );

        mockMvc.perform(post("/api/report/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("")
        );

        verify(moderationService, times(1))
                .createNewReport(request, "testFirebaseUid");
    }

    @Test
    @DisplayName("Test for user report duplicate discussion report")
    void duplicateOpenReport_returnsConflict() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");

        org.mockito.Mockito.doThrow(new RuntimeException("Report already exists"))
                .when(moderationService)
                .createNewReport(
                        org.mockito.ArgumentMatchers.any(ReportRequest.class),
                        org.mockito.ArgumentMatchers.eq("testFirebaseUid")
                );

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy comment",
                ReportCategoryName.SPAM,
                1L
        );

        mockMvc.perform(post("/api/report/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test for guest/unauthenticated user reporting. Expected 401")
    void returns401_whenNoAuthentication() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy comment",
                ReportCategoryName.SPAM,
                2L
        );

        mockMvc.perform(post("/api/report/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("Test for blank report reason. Expected 400")
    void returns400_whenReportReasonIsBlank() throws Exception {
        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "   ",
                ReportCategoryName.SPAM,
                2L
        );

        mockMvc.perform(post("/api/report/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("Test for reporting a missing discussion. Expected 404")
    void returns404_whenDiscussionDoesNotExist() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doThrow(new RuntimeException("Discussion not found"))
                .when(moderationService)
                .createNewReport(
                        org.mockito.ArgumentMatchers.any(ReportRequest.class),
                        org.mockito.ArgumentMatchers.eq("testFirebaseUid"));

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy comment",
                ReportCategoryName.SPAM,
                999L
        );

        mockMvc.perform(post("/api/report/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test for fetching all current open reports. Expected 200")
    void return200_whenCallGetReports() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");

        ReportsPayload payload = queuePayload();
        when(moderationService.getReportQueue("testFirebaseUid3")).thenReturn(payload);

        mockMvc.perform(get("/api/report/reports"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(payload)));

        verify(moderationService, times(1)).getReportQueue("testFirebaseUid3");
        verify(moderationService, never()).getReport(anyString(), anyLong());
    }

    @Test
    @DisplayName("Test for fetching report from a reportId")
    void return200_whenCallGetReportById() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        ReportsPayload payload = queuePayload();

        when(moderationService.getReport("testFirebaseUid3", 1L)).thenReturn(payload);

        mockMvc.perform(get("/api/report/reports")
                        .param("reportId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(payload)));

        verify(moderationService, times(1)).getReport("testFirebaseUid3", 1L);
        verify(moderationService, never()).getReportQueue(anyString());
    }

    @Test
    @DisplayName("GET /api/report/reports maps missing auth to 404")
    void return404_whenGetReportQueueUnauthenticated() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        mockMvc.perform(get("/api/report/reports"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Missing or invalid authentication token"));

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("GET /api/report/reports maps unauthorized role to 404")
    void return404_whenGetReportQueueUnauthorized() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        when(moderationService.getReportQueue("testFirebaseUid"))
                .thenThrow(new RuntimeException("Role CLIMBER is not allowed to perform action "));

        mockMvc.perform(get("/api/report/reports"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Role CLIMBER is not allowed to perform action "));

        verify(moderationService, times(1)).getReportQueue("testFirebaseUid");
        verify(moderationService, never()).getReport(anyString(), anyLong());
    }

    @Test
    @DisplayName("GET /api/report/reports?reportId= maps missing report to 404")
    void return404_whenGetReportByIdNotFound() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(moderationService.getReport("testFirebaseUid3", 999L))
                .thenThrow(new RuntimeException("Report not found"));

        mockMvc.perform(get("/api/report/reports").param("reportId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Report not found"));

        verify(moderationService, times(1)).getReport("testFirebaseUid3", 999L);
        verify(moderationService, never()).getReportQueue(anyString());
    }

    @Test
    @DisplayName("GET /api/report/reports?reportId= maps unauthorized role to 404")
    void return404_whenGetReportByIdUnauthorized() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        when(moderationService.getReport("testFirebaseUid", 1L))
                .thenThrow(new RuntimeException("Role CLIMBER is not allowed to perform action "));

        mockMvc.perform(get("/api/report/reports").param("reportId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Role CLIMBER is not allowed to perform action "));

        verify(moderationService, times(1)).getReport("testFirebaseUid", 1L);
        verify(moderationService, never()).getReportQueue(anyString());
    }

    private static ReportsPayload queuePayload() {
        UserAccountDTO reporter = new UserAccountDTO(2L, "sam", "sam@example.com", "CLIMBER");
        ReportUserDTO reportUser = new ReportUserDTO(
                11L,
                reporter,
                ReportCategoryName.SPAM,
                "Spammy comment",
                Instant.parse("2026-08-16T15:00:00Z")
        );
        ReportDTO report = new ReportDTO(
                ReportTargetType.DISCUSSION,
                new UserDiscussionData(
                        40L,
                        8L,
                        "alex",
                        null,
                        DiscussionType.COMMENT,
                        "hello",
                        LocalDateTime.of(2026, 8, 16, 10, 0)
                ),
                null,
                null,
                null,
                List.of(reportUser)
        );
        CategoryTallyDTO tally = new CategoryTallyDTO(ReportCategoryName.SPAM, 1, 2);
        return new ReportsPayload(List.of(new ReportPriorityDTO(report, List.of(tally), 2)));
    }
}
