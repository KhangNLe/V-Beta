package app.VBeta.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.discussions.PerceiveGradeRequest;
import app.VBeta.api.dto.discussions.comment.CommentDeletionRequest;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.discussions.video.SolutionBetaCreateRequest;
import app.VBeta.api.dto.discussions.video.SolutionBetaDeletionRequest;
import app.VBeta.api.dto.problems.ClimbingProblemDetailResponse;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.controller.ProblemDiscussionController;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.discussions.DiscussionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProblemDiscussionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ProblemDiscussionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    private ProblemDiscussionService problemDiscussionService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private ClimbingWallService climbingWallService;

    private UserDiscussionData sampleComment() {
        return new UserDiscussionData(
                101L,
                1L,
                "test-user",
                null,
                DiscussionType.COMMENT,
                "Nice",
                LocalDateTime.of(2026, 5, 7, 14, 0)
        );
    }

    @Test
    @DisplayName("POST /api/discussion/add-comments returns created comment")
    void returns201_whenAuthenticated() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        when(problemDiscussionService.addComment(eq("testFirebaseUid"), any(DiscussionCommentRequest.class)))
                .thenReturn(sampleComment());

        DiscussionCommentRequest req = new DiscussionCommentRequest(2L, "Nice");
        mockMvc.perform(post("/api/discussion/add-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discussionId").value(101L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.discussionType").value("COMMENT"))
                .andExpect(jsonPath("$.discussionContent").value("Nice"));
        verify(problemDiscussionService, times(1)).addComment(eq("testFirebaseUid"), any(DiscussionCommentRequest.class));
    }

    @Test
    @DisplayName("POST add-comments maps auth failure to 404")
    void returns404_whenNoAuthentication() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new RuntimeException("Missing or invalid authentication token"));

        DiscussionCommentRequest req = new DiscussionCommentRequest(2L, "Nice");
        mockMvc.perform(post("/api/discussion/add-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/discussion/solution-beta/upload-url returns signed URL")
    void returns200_whenRequestingUploadUrl() throws Exception {
        CloudFileStorageRequest request = new CloudFileStorageRequest(
                "beta_22.mp4", "video/mp4", 22L, 1L);
        when(problemDiscussionService.getSignedUrl(any(CloudFileStorageRequest.class)))
                .thenReturn(new CloudFileStorageResponse(
                        "https://storage.googleapis.com/signed",
                        "PUT",
                        "wallSection-1/problem-22/uuid-beta_22.mp4",
                        "https://storage.googleapis.com/bucket/video.mp4"
                ));

        mockMvc.perform(get("/api/discussion/solution-beta/upload-url")
                        .param("fileName", request.fileName())
                        .param("contentType", request.contentType())
                        .param("problemId", String.valueOf(request.problemId()))
                        .param("wallSectionId", String.valueOf(request.wallSectionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.signedURL").value("https://storage.googleapis.com/signed"));
    }

    @Test
    @DisplayName("POST suggest-grade returns updated problem detail")
    void returns201_whenSuggestingGrade() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(authorizationService).authorize("testFirebaseUid", ActionDefinition.GRADE_PROBLEM);
        doNothing().when(problemDiscussionService)
                .addClimbingProblemPerceiveGrade(eq("testFirebaseUid"), eq(22L), any(PerceiveGradeRequest.class));
        when(climbingWallService.getClimbingProblem(22L)).thenReturn(
                new ClimbingProblemDetailResponse(
                        new ClimbingProblemResponse(22L, "BLUE", "Crimpy sequence", "2026-04-20", GradeDefinition.V5, null),
                        "V6",
                        List.of()
                )
        );

        mockMvc.perform(post("/api/discussion/problems/22/suggest-grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PerceiveGradeRequest(GradeDefinition.V6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perceiveGrade").value("V6"));
    }

    @Test
    @DisplayName("POST suggest-grade maps authorization failure to 404")
    void returns404_whenSuggestGradeUnauthorized() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doThrow(new RuntimeException("Role CLIMBER is not allowed to perform action GRADE_PROBLEM"))
                .when(authorizationService).authorize("testFirebaseUid", ActionDefinition.GRADE_PROBLEM);

        mockMvc.perform(post("/api/discussion/problems/22/suggest-grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PerceiveGradeRequest(GradeDefinition.V6))))
                .andExpect(status().isNotFound());

        verify(problemDiscussionService, never())
                .addClimbingProblemPerceiveGrade(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/discussion/solution-beta/save returns created beta")
    void returns201_whenSavingSolutionBeta() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        when(problemDiscussionService.saveSolutionBeta(any(SolutionBetaCreateRequest.class), eq("testFirebaseUid")))
                .thenReturn(new UserDiscussionData(
                        402L, 1L, "test-user", null, DiscussionType.BETA,
                        "https://storage.googleapis.com/bucket/video.mp4",
                        LocalDateTime.of(2026, 4, 20, 20, 2)
                ));

        SolutionBetaCreateRequest request = new SolutionBetaCreateRequest(
                22L,
                "wallSection-1/problem-22/uuid-beta_22.mp4",
                "https://storage.googleapis.com/bucket/video.mp4"
        );

        mockMvc.perform(post("/api/discussion/solution-beta/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discussionId").value(402))
                .andExpect(jsonPath("$.discussionType").value("BETA"));
    }

    @Test
    @DisplayName("DELETE /api/discussion/solution-beta returns 200")
    void returns200_whenDeletingSolutionBeta() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(problemDiscussionService)
                .softDeleteUserSolutionBeta(any(SolutionBetaDeletionRequest.class), eq("testFirebaseUid"));

        SolutionBetaDeletionRequest request = new SolutionBetaDeletionRequest(
                1L, 22L, 402L, "https://storage.googleapis.com/bucket/video.mp4",
                "User deleted their own discussion");

        mockMvc.perform(delete("/api/discussion/solution-beta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(problemDiscussionService, times(1))
                .softDeleteUserSolutionBeta(any(SolutionBetaDeletionRequest.class), eq("testFirebaseUid"));
    }

    @Test
    @DisplayName("DELETE /api/discussion/comment/delete returns 200")
    void returns200_whenDeletingComment() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(authorizationService).authorize("testFirebaseUid", ActionDefinition.DELETE_COMMENT);
        doNothing().when(problemDiscussionService)
                .softDeleteUserComment(eq("testFirebaseUid"), any(CommentDeletionRequest.class));

        CommentDeletionRequest request = new CommentDeletionRequest(
                1L, 22L, 101L, "Nice", "User deleted their own discussion");

        mockMvc.perform(delete("/api/discussion/comment/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(problemDiscussionService, times(1))
                .softDeleteUserComment(eq("testFirebaseUid"), any(CommentDeletionRequest.class));
    }

    @Test
    @DisplayName("DELETE comment maps service failure to 404")
    void returns404_whenDeleteCommentFails() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(authorizationService).authorize("testFirebaseUid", ActionDefinition.DELETE_COMMENT);
        doThrow(new RuntimeException("Invalid Action. Cannot remove object from different author"))
                .when(problemDiscussionService)
                .softDeleteUserComment(eq("testFirebaseUid"), any(CommentDeletionRequest.class));

        CommentDeletionRequest request = new CommentDeletionRequest(
                1L, 22L, 101L, "Nice", "User deleted their own discussion");

        mockMvc.perform(delete("/api/discussion/comment/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
