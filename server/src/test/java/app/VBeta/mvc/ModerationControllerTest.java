package app.VBeta.mvc;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.controller.ModerationController;
import app.VBeta.domain.model.moderation.ModerateActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ModerationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ModerationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("POST /api/moderate/report return 200 OK after admin decision")
    void returns200_whenAuthenticatedAdminModerateAReport() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        doNothing().when(moderationService).createModerationForReportQueue(
                org.mockito.ArgumentMatchers.any(ModerationRequest.class),
                org.mockito.ArgumentMatchers.eq("testFirebaseUid3")
        );

        ModerationRequest request = new ModerationRequest(
                List.of(1L, 2L, 3L),
                ModerateActionType.REPORT_DISMISSED,
                "Nothing wrong with this content"
        );

        mockMvc.perform(post("/api/moderate/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(moderationService, times(1)).createModerationForReportQueue(request,
                "testFirebaseUid3");

    }

    @Test
    @DisplayName("POST /api/moderation/report with 404 from unauthorize user")
    void returns404_whenAuthenticatedAdminModerateAReportNotFound() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenThrow(
                new RuntimeException("Missing or invalid authentication token")
        );

        ModerationRequest request = new ModerationRequest(
                List.of(1L, 2L, 3L),
                ModerateActionType.REPORT_DISMISSED,
                "Nothing wrong with this content"
        );

        mockMvc.perform(post("/api/moderate/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/moderation/report with no report number")
    void returns400_whenAuthenticatedAdminModerateAReportNumberNotFound() throws Exception {
        ModerationRequest request = new ModerationRequest(
                null,
                ModerateActionType.REPORT_DISMISSED,
                "Nothing wrong with this content"
        );

        mockMvc.perform(post("/api/moderate/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/moderation/report with blank decision reason.")
    void returns400_whenAuthenticatedAdminModerateAReportBlankReason() throws Exception {
        ModerationRequest request = new ModerationRequest(
                List.of(1L),
                ModerateActionType.REPORT_DISMISSED,
                null
        );

        mockMvc.perform(post("/api/moderate/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/moderation/report with no moderate action.")
    void returns400_whenAuthenticatedAdminModerateAReportNoModerateAction() throws Exception {
        ModerationRequest request = new ModerationRequest(
                List.of(1L),
                null,
                "Meh idk"
        );

        mockMvc.perform(post("/api/moderate/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
