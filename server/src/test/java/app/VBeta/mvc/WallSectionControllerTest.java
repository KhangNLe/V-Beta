package app.VBeta.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.problems.ClimbingProblemCreationRequest;
import app.VBeta.api.dto.problems.ClimbingProblemDetailResponse;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.api.dto.walls.WallSectionResponse;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.controller.WallSectionController;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.GradeDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WallSectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class WallSectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClimbingWallService climbingWallService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private ClimbingProblemResponse sampleProblem() {
        return new ClimbingProblemResponse(22L, "BLUE", "Crimpy sequence", "2026-04-20", GradeDefinition.V5);
    }

    @Test
    @DisplayName("GET /api/home/wall-sections returns wall list")
    void returns200_whenListingWallSections() throws Exception {
        when(climbingWallService.getWallSections()).thenReturn(List.of(
                new WallSectionResponse(1L, "Main Wall", "Comp style problems")
        ));

        mockMvc.perform(get("/api/home/wall-sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wallSectionID").value(1))
                .andExpect(jsonPath("$[0].wallSectionName").value("Main Wall"));
    }

    @Test
    @DisplayName("GET /api/home/wall-sections/{id}/problems returns problems")
    void returns200_whenListingProblemsForWall() throws Exception {
        when(climbingWallService.getClimbingProblemsByWallSectionId(1L))
                .thenReturn(List.of(sampleProblem()));

        mockMvc.perform(get("/api/home/wall-sections/1/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(22));
    }

    @Test
    @DisplayName("GET problem list maps missing wall to 404")
    void returns404_whenWallSectionIsMissing() throws Exception {
        when(climbingWallService.getClimbingProblemsByWallSectionId(999L))
                .thenThrow(new RuntimeException("Wall Section with id 999 does not exist."));

        mockMvc.perform(get("/api/home/wall-sections/999/problems"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET problem detail returns 200")
    void returns200_whenGettingProblemDetail() throws Exception {
        when(climbingWallService.getClimbingProblem(22L)).thenReturn(
                new ClimbingProblemDetailResponse(sampleProblem(), "V5", List.of())
        );

        mockMvc.perform(get("/api/home/wall-sections/1/problems/22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.climbingProblem.problemId").value(22))
                .andExpect(jsonPath("$.perceiveGrade").value("V5"));
    }

    @Test
    @DisplayName("POST /api/home/wall-section/creation returns 201")
    void returns201_whenCreatingWallSection() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("adminUid");
        doNothing().when(authorizationService).authorize("adminUid", ActionDefinition.CREATE_WALL);
        when(climbingWallService.createNewWallSection(any(WallSectionCreationRequest.class)))
                .thenReturn(new WallSectionResponse(5L, "Training Wall", "Endurance circuits"));

        WallSectionCreationRequest request = new WallSectionCreationRequest(
                "Endurance circuits", "Training Wall");

        mockMvc.perform(post("/api/home/wall-section/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wallSectionID").value(5));
    }

    @Test
    @DisplayName("POST wall creation maps authorization failure to 400")
    void returns400_whenCreateWallIsUnauthorized() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("climberUid");
        doThrow(new RuntimeException("Role CLIMBER is not allowed to perform action CREATE_WALL"))
                .when(authorizationService).authorize("climberUid", ActionDefinition.CREATE_WALL);

        WallSectionCreationRequest request = new WallSectionCreationRequest(
                "Endurance circuits", "Training Wall");

        mockMvc.perform(post("/api/home/wall-section/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(climbingWallService, never()).createNewWallSection(any());
    }

    @Test
    @DisplayName("DELETE wall section returns 200")
    void returns200_whenDeletingWallSection() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("adminUid");
        doNothing().when(authorizationService).authorize("adminUid", ActionDefinition.DELETE_WALL);
        doNothing().when(climbingWallService).deleteWallSection(5L);

        mockMvc.perform(delete("/api/home/wall-section/5/delete"))
                .andExpect(status().isOk());

        verify(climbingWallService, times(1)).deleteWallSection(5L);
    }

    @Test
    @DisplayName("PATCH wall reset returns 200")
    void returns200_whenResettingWallSection() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("setterUid");
        doNothing().when(authorizationService).authorize("setterUid", ActionDefinition.RESET_WALL);
        doNothing().when(climbingWallService).resetWallSection(1L);

        mockMvc.perform(patch("/api/home/wall-section/1/reset"))
                .andExpect(status().isOk());

        verify(climbingWallService, times(1)).resetWallSection(1L);
    }

    @Test
    @DisplayName("POST create climbing problem returns 201")
    void returns201_whenCreatingClimbingProblem() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("setterUid");
        doNothing().when(authorizationService).authorize("setterUid", ActionDefinition.CREATE_PROBLEM);
        when(climbingWallService.createNewClimbingProblem(eq(1L), any(ClimbingProblemCreationRequest.class)))
                .thenReturn(sampleProblem());

        ClimbingProblemCreationRequest request = new ClimbingProblemCreationRequest(
                "BLUE", "Crimpy sequence", GradeDefinition.V5);

        mockMvc.perform(post("/api/home/wall-sections/1/problems/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.problemId").value(22));
    }

    @Test
    @DisplayName("PATCH delete climbing problem returns remaining problems")
    void returns200_whenDeletingClimbingProblem() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("setterUid");
        doNothing().when(authorizationService).authorize("setterUid", ActionDefinition.DELETE_PROBLEM);
        doNothing().when(climbingWallService).deleteClimbingProblem(22L);
        when(climbingWallService.getClimbingProblemsByWallSectionId(1L)).thenReturn(List.of());

        mockMvc.perform(patch("/api/home/wall-sections/1/problems/22/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(climbingWallService, times(1)).deleteClimbingProblem(22L);
    }
}
