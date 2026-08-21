package app.VBeta.mvc;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.moderation.ModerationDTO;
import app.VBeta.api.dto.moderation.ModerationPayload;
import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.api.dto.report.ReportUserDTO;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.controller.ModerationController;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private ModerationDTO sampleModerationDTO(ModerateActionType moderateActionType) {
        return new ModerationDTO(
                1L,
                new ReportDTO(
                    ReportTargetType.DISCUSSION,
                        new UserDiscussionData(
                            1L,
                                45L,
                                "spammyUser",
                                null,
                                DiscussionType.COMMENT,
                                "This is a spam comment",
                                LocalDateTime.now()
                        ),
                        null,
                        null,
                        null,
                        List.of(new ReportUserDTO(
                                99L,
                                new UserAccountDTO(
                                        123L,
                                        "testUser",
                                        "testUser@gmail.com",
                                        "CLIMBER"
                                ),
                                ReportCategoryName.SPAM,
                                "It's spammy",
                                Instant.now()
                        ))
                ),
                new UserAccountDTO(
                    20L,
                        "adminUser",
                        "adminUser@gmail.com",
                        "ADMIN"
                ),
                moderateActionType,
                "This is a test",
                Instant.now()
                );
    }

    private ModerationPayload samplePayload(){
        return new ModerationPayload(List.of(sampleModerationDTO(ModerateActionType.REPORT_DISMISSED)));
    }

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

    @Test
    @DisplayName("GET /api/moderation/logbook?moderationId= return correct moderate DTO")
    void returns200_whenAuthenticateAdminRequestModerationLogWithId() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(moderationService.getModerationLog("testFirebaseUid3", 1L))
                .thenReturn(samplePayload());

        mockMvc.perform(get("/api/moderate/logbook").param("moderationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationLogs[0].moderationId").value(1))
                .andExpect(jsonPath("$.moderationLogs.[0].report.targetType").value(ReportTargetType.DISCUSSION.name()))
                .andExpect(jsonPath("$.moderationLogs.[0].report.discussion.discussionId").value(1))
                .andExpect(jsonPath("$.moderationLogs.[0].resolvedBy.username").value("adminUser"))
                .andExpect(jsonPath("$.moderationLogs.[0].decision").value(ModerateActionType.REPORT_DISMISSED.name()));

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verify(moderationService, times(1)).getModerationLog("testFirebaseUid3", 1L);
        verify(moderationService, times(0)).getLogbook("testFirebaseUid3", 1);
    }

    @Test
    @DisplayName("GET /api/moderation/logbook return list of moderation logs")
    void returns200_whenAuthenticateAdminRequestModerationLogs() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(moderationService.getLogbook("testFirebaseUid3", 1)).thenReturn(samplePayload());

        mockMvc.perform(get("/api/moderate/logbook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationLogs[0].moderationId").value(1))
                .andExpect(jsonPath("$.moderationLogs.[0].report.targetType").value(ReportTargetType.DISCUSSION.name()))
                .andExpect(jsonPath("$.moderationLogs.[0].report.discussion.discussionId").value(1))
                .andExpect(jsonPath("$.moderationLogs.[0].resolvedBy.username").value("adminUser"))
                .andExpect(jsonPath("$.moderationLogs.[0].decision").value(ModerateActionType.REPORT_DISMISSED.name()));

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verify(moderationService, times(1)).getLogbook("testFirebaseUid3", 1);
        verify(moderationService, times(0)).getModerationLog("testFirebaseUid3", 1L);
    }

    @Test
    @DisplayName("GET /api/moderation/logbook return 404 when missing authentication token")
    void returns404_whenMissingAuthenticationToken() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenThrow(RuntimeException.class);

        mockMvc.perform(get("/api/moderate/logbook"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("GET /api/moderation/logbook?moderationId= returns 404 when missing authentication token")
    void returns404_whenMissingAuthToken() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenThrow(RuntimeException.class);

        mockMvc.perform(get("/api/moderate/logbook").param("moderationId", "1"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("GET /api/moderation/logbook?moderationId= return 404 when have wrong moderation id")
    void returns404_whenProvideWrongModerationId() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(moderationService.getModerationLog("testFirebaseUid3", 1L))
                .thenThrow(RuntimeException.class);

        mockMvc.perform(get("/api/moderate/logbook").param("moderationId", "1"))
                .andExpect(status().isNotFound());

        verify(authorizationService, times(1)).getAuthenticatedFirebaseUid();
        verify(moderationService, times(1)).getModerationLog("testFirebaseUid3", 1L);
        verify(moderationService, times(0)).getLogbook("testFirebaseUid3", 1);
    }
}
