package app.VBeta;

import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.report.*;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.controller.ContentReportController;
import app.VBeta.domain.model.report.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
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
                .andExpect(status().isCreated())
                .andExpect(content().string("")
        );

        verify(moderationService, times(1))
                .createNewReport(request, "testFirebaseUid");
    }

    @Test
    @DisplayName("Test for user report duplicate discussion report")
    void duplicateOpenReport_returnsConflict() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");

        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Report already exist"))
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
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Test for guest/unauthenticated user reporting. Expected 401")
    void returns401_whenNoAuthentication() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token"));

        ReportRequest request = new ReportRequest(
                ReportTargetType.DISCUSSION,
                "Spammy comment",
                ReportCategoryName.SPAM,
                2L
        );

        mockMvc.perform(post("/api/report/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

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
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
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
}
