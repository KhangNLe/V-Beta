package app.VBeta.mvc;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.moderation.AppealDTO;
import app.VBeta.api.dto.moderation.AppealPayload;
import app.VBeta.api.dto.moderation.AppealRequest;
import app.VBeta.api.dto.moderation.ModerateAppealRequest;
import app.VBeta.api.dto.moderation.ModerationDTO;
import app.VBeta.api.dto.moderation.ModerationPayload;
import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.api.dto.moderation.OwnerDeletionNoticeDTO;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.api.dto.report.ReportUserDTO;
import app.VBeta.application.AppealService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ModerationService;
import app.VBeta.controller.ModerationController;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private AppealService appealService;

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

    private AppealDTO sampleAppealDTO() {
        return new AppealDTO(
                7L,
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
                        45L,
                        "spammyUser",
                        "spammyUser@gmail.com",
                        "CLIMBER"
                ),
                "This was a joke, please restore."
        );
    }

    private AppealPayload sampleAppealPayload() {
        return new AppealPayload(List.of(sampleAppealDTO()));
    }

    private ReportDTO sampleOwnerNoticeReport() {
        ReportDTO source = sampleAppealDTO().report();
        ReportUserDTO flag = source.reporters().get(0);
        return new ReportDTO(
                source.targetType(),
                source.discussion(),
                source.climbingProblem(),
                source.wallSection(),
                source.user(),
                List.of(new ReportUserDTO(
                        flag.reportId(),
                        null,
                        flag.categoryName(),
                        flag.reportReason(),
                        flag.createdAt()
                ))
        );
    }

    @Test
    @DisplayName("POST /api/moderate/appeal returns 201 after owner submits an appeal")
    void returns201_whenAuthenticatedOwnerCreatesAppeal() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(appealService).createAppeal(
                org.mockito.ArgumentMatchers.any(AppealRequest.class),
                org.mockito.ArgumentMatchers.eq("testFirebaseUid")
        );

        AppealRequest request = new AppealRequest(99L, "This was a joke, please restore.");

        mockMvc.perform(post("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(appealService, times(1)).createAppeal(request, "testFirebaseUid");
    }

    @Test
    @DisplayName("POST /api/moderate/appeal returns 400 when reason is blank")
    void returns400_whenAppealReasonIsBlank() throws Exception {
        AppealRequest request = new AppealRequest(99L, " ");

        mockMvc.perform(post("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("POST /api/moderate/appeal returns 404 when the service rejects the appeal")
    void returns404_whenAppealCreateFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doThrow(new RuntimeException("Appeal is not allowed")).when(appealService)
                .createAppeal(org.mockito.ArgumentMatchers.any(AppealRequest.class),
                        org.mockito.ArgumentMatchers.eq("testFirebaseUid"));

        AppealRequest request = new AppealRequest(99L, "This was a joke, please restore.");

        mockMvc.perform(post("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/moderate/appeal/notice returns the owner deletion notice")
    void returns200_whenOwnerRequestsDeletionNotice() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        when(appealService.getOwnerDeletionNotice(11L, "testFirebaseUid")).thenReturn(
                new OwnerDeletionNoticeDTO(
                        11L,
                        ReportStatus.CONTENT_REMOVED,
                        "Does not belong on this wall.",
                        sampleOwnerNoticeReport(),
                        null,
                        true,
                        null
                )
        );

        mockMvc.perform(get("/api/moderate/appeal/notice").param("reportId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(11))
                .andExpect(jsonPath("$.adminReason").value("Does not belong on this wall."))
                .andExpect(jsonPath("$.canAppeal").value(true))
                .andExpect(jsonPath("$.appeal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.report.reporters[0].categoryName").value("SPAM"))
                .andExpect(jsonPath("$.report.reporters[0].reportReason").value("It's spammy"))
                .andExpect(jsonPath("$.report.reporters[0].reporter").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("GET /api/moderate/appeal?appealId= returns the matching appeal")
    void returns200_whenAdminRequestsAppealById() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(appealService.getUserAppeal(7L, "testFirebaseUid3")).thenReturn(sampleAppealPayload());

        mockMvc.perform(get("/api/moderate/appeal").param("appealId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeals[0].appealId").value(7))
                .andExpect(jsonPath("$.appeals[0].report.targetType").value(ReportTargetType.DISCUSSION.name()))
                .andExpect(jsonPath("$.appeals[0].appealUser.username").value("spammyUser"))
                .andExpect(jsonPath("$.appeals[0].appealReason").value("This was a joke, please restore."));

        verify(appealService, times(1)).getUserAppeal(7L, "testFirebaseUid3");
        verify(appealService, times(0)).getAppeals("testFirebaseUid3");
    }

    @Test
    @DisplayName("GET /api/moderate/appeal?reportId= returns the matching appeal")
    void returns200_whenAdminRequestsAppealByReportId() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(appealService.getAppealByReport(11L, "testFirebaseUid3")).thenReturn(sampleAppealPayload());

        mockMvc.perform(get("/api/moderate/appeal").param("reportId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeals[0].appealId").value(7))
                .andExpect(jsonPath("$.appeals[0].appealUser.username").value("spammyUser"))
                .andExpect(jsonPath("$.appeals[0].appealReason").value("This was a joke, please restore."));

        verify(appealService, times(1)).getAppealByReport(11L, "testFirebaseUid3");
        verify(appealService, times(0)).getAppeals("testFirebaseUid3");
    }

    @Test
    @DisplayName("GET /api/moderate/appeal returns the open appeal queue")
    void returns200_whenAdminRequestsAppealQueue() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(appealService.getAppeals("testFirebaseUid3")).thenReturn(sampleAppealPayload());

        mockMvc.perform(get("/api/moderate/appeal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeals[0].appealId").value(7))
                .andExpect(jsonPath("$.appeals[0].appealUser.username").value("spammyUser"));

        verify(appealService, times(1)).getAppeals("testFirebaseUid3");
        verify(appealService, times(0)).getUserAppeal(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("GET /api/moderate/appeal returns 404 when missing authentication token")
    void returns404_whenAppealQueueMissingAuth() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenThrow(RuntimeException.class);

        mockMvc.perform(get("/api/moderate/appeal"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("GET /api/moderate/appeal?appealId= returns 404 when the appeal is missing")
    void returns404_whenAppealIdIsUnknown() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        when(appealService.getUserAppeal(7L, "testFirebaseUid3"))
                .thenThrow(new RuntimeException("Appeal not found"));

        mockMvc.perform(get("/api/moderate/appeal").param("appealId", "7"))
                .andExpect(status().isNotFound());

        verify(appealService, times(1)).getUserAppeal(7L, "testFirebaseUid3");
        verify(appealService, times(0)).getAppeals("testFirebaseUid3");
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 200 after admin approves an appeal")
    void returns200_whenAdminApprovesAppeal() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        doNothing().when(appealService).moderateAppeal(
                org.mockito.ArgumentMatchers.any(ModerateAppealRequest.class),
                org.mockito.ArgumentMatchers.eq("testFirebaseUid3")
        );

        ModerateAppealRequest request = new ModerateAppealRequest(
                7L, AppealStatus.APPROVED, "Restored after review.");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(appealService, times(1)).moderateAppeal(request, "testFirebaseUid3");
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 200 after admin denies an appeal")
    void returns200_whenAdminDeniesAppeal() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        doNothing().when(appealService).moderateAppeal(
                org.mockito.ArgumentMatchers.any(ModerateAppealRequest.class),
                org.mockito.ArgumentMatchers.eq("testFirebaseUid3")
        );

        ModerateAppealRequest request = new ModerateAppealRequest(
                7L, AppealStatus.DENIED, "Removal stands.");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(appealService, times(1)).moderateAppeal(request, "testFirebaseUid3");
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 400 when admin notes are blank")
    void returns400_whenAppealDecisionReasonIsBlank() throws Exception {
        ModerateAppealRequest request = new ModerateAppealRequest(7L, AppealStatus.DENIED, " ");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 400 when appeal id is missing")
    void returns400_whenAppealDecisionIdIsMissing() throws Exception {
        ModerateAppealRequest request = new ModerateAppealRequest(null, AppealStatus.APPROVED, "Restored.");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 400 when status is OPEN")
    void returns400_whenAppealDecisionStatusIsOpen() throws Exception {
        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"appealId":7,"appealStatus":"OPEN","adminReason":"Not a decision."}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 404 when the service rejects the decision")
    void returns404_whenAppealDecisionFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid3");
        doThrow(new RuntimeException("Appeal not found")).when(appealService)
                .moderateAppeal(org.mockito.ArgumentMatchers.any(ModerateAppealRequest.class),
                        org.mockito.ArgumentMatchers.eq("testFirebaseUid3"));

        ModerateAppealRequest request = new ModerateAppealRequest(
                7L, AppealStatus.APPROVED, "Restored after review.");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/moderate/appeal returns 404 when missing authentication token")
    void returns404_whenAppealDecisionMissingAuth() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenThrow(RuntimeException.class);

        ModerateAppealRequest request = new ModerateAppealRequest(
                7L, AppealStatus.DENIED, "Removal stands.");

        mockMvc.perform(patch("/api/moderate/appeal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(appealService);
    }
}
