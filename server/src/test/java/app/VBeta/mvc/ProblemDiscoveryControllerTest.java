package app.VBeta.mvc;

import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.application.ProblemFilteringService;
import app.VBeta.controller.ProblemDiscoveryController;
import app.VBeta.domain.model.climb.GradeDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProblemDiscoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ProblemDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemFilteringService problemFilteringService;

    private ClimbingProblemResponse sampleProblem() {
        return new ClimbingProblemResponse(2L, "RED", "RED V0-V1", "2026-07-21T12:00:00", GradeDefinition.V0);
    }

    @Test
    @DisplayName("GET /api/search/{id} returns unsorted problems")
    void returns200_whenFilteringUnsorted() throws Exception {
        when(problemFilteringService.findProblemsByRange(1L, GradeDefinition.V0, GradeDefinition.V5))
                .thenReturn(List.of(sampleProblem()));

        mockMvc.perform(get("/api/search/1").param("min", "V0").param("max", "V5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(2))
                .andExpect(jsonPath("$[0].assignedGrade").value("V0"));

        verify(problemFilteringService, times(1))
                .findProblemsByRange(1L, GradeDefinition.V0, GradeDefinition.V5);
        verify(problemFilteringService, never()).findProblemBetweenRangeAsc(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/search/{id}?sort=asc uses ascending filter")
    void returns200_whenSortingAscending() throws Exception {
        when(problemFilteringService.findProblemBetweenRangeAsc(1L, GradeDefinition.V0, GradeDefinition.V5))
                .thenReturn(List.of(sampleProblem()));

        mockMvc.perform(get("/api/search/1")
                        .param("min", "V0")
                        .param("max", "V5")
                        .param("sort", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(2));

        verify(problemFilteringService, times(1))
                .findProblemBetweenRangeAsc(1L, GradeDefinition.V0, GradeDefinition.V5);
    }

    @Test
    @DisplayName("GET /api/search/{id}?sort=desc uses descending filter")
    void returns200_whenSortingDescending() throws Exception {
        when(problemFilteringService.findProblemBetweenRangeDesc(1L, GradeDefinition.V0, GradeDefinition.V5))
                .thenReturn(List.of(sampleProblem()));

        mockMvc.perform(get("/api/search/1")
                        .param("min", "V0")
                        .param("max", "V5")
                        .param("sort", "desc"))
                .andExpect(status().isOk());

        verify(problemFilteringService, times(1))
                .findProblemBetweenRangeDesc(1L, GradeDefinition.V0, GradeDefinition.V5);
    }

    @Test
    @DisplayName("GET /api/search/{id} maps missing wall to 404")
    void returns404_whenWallSectionIsMissing() throws Exception {
        when(problemFilteringService.findProblemsByRange(999L, GradeDefinition.V0, GradeDefinition.V5))
                .thenThrow(new RuntimeException("Unable to find the wall section"));

        mockMvc.perform(get("/api/search/999").param("min", "V0").param("max", "V5"))
                .andExpect(status().isNotFound());
    }
}
