package app.VBeta.Integration_Test;

import app.VBeta.api.dto.problems.ClimbingProblemCreationRequest;
import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.walls.WallSectionCreationRequest;
import app.VBeta.application.ClimbingWallService;
import app.VBeta.application.ProblemFilteringService;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.config.TestGcpStorageConfig;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.GradeDefinition;
import app.VBeta.domain.model.climb.WallSection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class ClimbingProblemFilteringTest {
    @Autowired
    private ProblemFilteringService problemFilteringService;

    @Autowired
    private ClimbingWallService climbingWallService;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private WallSectionManager wallSectionManager;

    @Autowired
    private MockMvc mockMvc;

    private WallSection createWallSection(String name) {
        return wallSectionManager.createNewWallSection(
                new WallSectionCreationRequest("Filtering test wall", name, null, null)
        );
    }

    private ClimbingProblemResponse createProblem(Long wallSectionId, String color, GradeDefinition grade) {
        return climbingWallService.createNewClimbingProblem(
                wallSectionId,
                new ClimbingProblemCreationRequest(color, color + " " + grade.name(), grade, null, null)
        );
    }

    private void archiveProblem(Long problemId) {
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        assertNotNull(problem);
        climbingProblemManager.archiveActiveProblems(List.of(problem));
    }

    private boolean checkForResponseOrderAsc(List<ClimbingProblemResponse> responses){
        for (int i = 1; i < responses.size(); i++) {
            if (responses.get(i-1).assignedGrade().compareTo(responses.get(i).assignedGrade()) > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkForResponseOrderDesc(List<ClimbingProblemResponse> responses){
        for (int i = 1; i < responses.size(); i++) {
            if (responses.get(i-1).assignedGrade().compareTo(responses.get(i).assignedGrade()) < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkForResponsesWithinRange(List<ClimbingProblemResponse> responses,
                                                GradeDefinition low, GradeDefinition high){
        for (ClimbingProblemResponse response : responses) {
            GradeDefinition responseGrade = response.assignedGrade();
            if (responseGrade.compareTo(low) < 0 || responseGrade.compareTo(high) > 0){
                return false;
            }
        }
        return true;
    }

    private Set<GradeDefinition> gradesOf(List<ClimbingProblemResponse> responses) {
        return responses.stream()
                .map(ClimbingProblemResponse::assignedGrade)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static Stream<Arguments> validGradePairs() {
        return Stream.of(
                Arguments.of(GradeDefinition.VB, GradeDefinition.VB),
                Arguments.of(GradeDefinition.VB, GradeDefinition.V17),
                Arguments.of(GradeDefinition.V0, GradeDefinition.V5),
                Arguments.of(GradeDefinition.V5, GradeDefinition.V5),
                Arguments.of(GradeDefinition.V10, GradeDefinition.V17)
        );
    }

    static Stream<Arguments> invalidGradePairs() {
        return Stream.of(
                Arguments.of(GradeDefinition.V5, GradeDefinition.V0),
                Arguments.of(GradeDefinition.V17, GradeDefinition.VB),
                Arguments.of(GradeDefinition.V10, GradeDefinition.V2)
        );
    }

    private void assertBadRequestForInvalidRange(
            Long wallSectionId,
            GradeDefinition minGrade,
            GradeDefinition maxGrade,
            TriGradeQuery query) {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                query.apply(wallSectionId, minGrade, maxGrade));
    }

    @FunctionalInterface
    private interface TriGradeQuery {
        List<ClimbingProblemResponse> apply(Long wallSectionId, GradeDefinition min, GradeDefinition max);
    }

    @ParameterizedTest
    @MethodSource("validGradePairs")
    @DisplayName("Valid grade range returns only in-range problems")
    void testForClimbingProblemBetweenRange(GradeDefinition minGrade, GradeDefinition maxGrade){
        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                1L, minGrade, maxGrade);
        assertTrue(checkForResponsesWithinRange(responses, minGrade, maxGrade));
    }

    @ParameterizedTest
    @MethodSource("invalidGradePairs")
    @DisplayName("Invalid grade range returns 400 for unsorted filter")
    void testInvalidRangeUnsorted(GradeDefinition minGrade, GradeDefinition maxGrade){
        assertBadRequestForInvalidRange(1L, minGrade, maxGrade, problemFilteringService::findProblemsByRange);
    }

    @ParameterizedTest
    @MethodSource("validGradePairs")
    @DisplayName("Valid grade range with ascending sort")
    void testForClimbingProblemBetweenRangeInAscendingOrder(GradeDefinition minGrade, GradeDefinition maxGrade){
        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemBetweenRangeAsc(
                1L, minGrade, maxGrade);
        assertTrue(checkForResponsesWithinRange(responses, minGrade, maxGrade));
        assertTrue(checkForResponseOrderAsc(responses));
    }

    @ParameterizedTest
    @MethodSource("invalidGradePairs")
    @DisplayName("Invalid grade range returns 400 for ascending filter")
    void testInvalidRangeAscending(GradeDefinition minGrade, GradeDefinition maxGrade){
        assertBadRequestForInvalidRange(1L, minGrade, maxGrade, problemFilteringService::findProblemBetweenRangeAsc);
    }

    @ParameterizedTest
    @MethodSource("validGradePairs")
    @DisplayName("Valid grade range with descending sort")
    void testForClimbingProblemBetweenRangeInDescendingOrder(GradeDefinition minGrade, GradeDefinition maxGrade){
        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemBetweenRangeDesc(
                1L, minGrade, maxGrade);
        assertTrue(checkForResponsesWithinRange(responses, minGrade, maxGrade));
        assertTrue(checkForResponseOrderDesc(responses));
    }

    @ParameterizedTest
    @MethodSource("invalidGradePairs")
    @DisplayName("Invalid grade range returns 400 for descending filter")
    void testInvalidRangeDescending(GradeDefinition minGrade, GradeDefinition maxGrade){
        assertBadRequestForInvalidRange(1L, minGrade, maxGrade, problemFilteringService::findProblemBetweenRangeDesc);
    }

    @Test
    @DisplayName("Excludes archived problems from grade-range results")
    void excludesArchivedProblemsFromRange() {
        WallSection wall = createWallSection("Archive Filter Wall");
        createProblem(wall.getId(), "Green", GradeDefinition.V2);
        ClimbingProblemResponse archived = createProblem(wall.getId(), "Red", GradeDefinition.V3);
        archiveProblem(archived.problemId());

        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                wall.getId(), GradeDefinition.V2, GradeDefinition.V3);

        assertEquals(Set.of(GradeDefinition.V2), gradesOf(responses));
        assertTrue(responses.stream().noneMatch(r -> r.problemId().equals(archived.problemId())));
    }

    @Test
    @DisplayName("Includes problems exactly at min and max grade bounds")
    void includesInclusiveGradeBounds() {
        WallSection wall = createWallSection("Inclusive Bounds Wall");
        createProblem(wall.getId(), "Black", GradeDefinition.VB);
        ClimbingProblemResponse minBound = createProblem(wall.getId(), "Blue", GradeDefinition.V1);
        ClimbingProblemResponse mid = createProblem(wall.getId(), "Yellow", GradeDefinition.V3);
        ClimbingProblemResponse maxBound = createProblem(wall.getId(), "Orange", GradeDefinition.V5);
        createProblem(wall.getId(), "Purple", GradeDefinition.V7);

        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                wall.getId(), GradeDefinition.V1, GradeDefinition.V5);

        Set<Long> ids = responses.stream().map(ClimbingProblemResponse::problemId).collect(Collectors.toSet());
        assertEquals(Set.of(minBound.problemId(), mid.problemId(), maxBound.problemId()), ids);
        assertEquals(Set.of(GradeDefinition.V1, GradeDefinition.V3, GradeDefinition.V5), gradesOf(responses));
    }

    @Test
    @DisplayName("Supports single-grade range when min equals max")
    void supportsSingleGradeRange() {
        WallSection wall = createWallSection("Single Grade Wall");
        ClimbingProblemResponse onlyV4 = createProblem(wall.getId(), "White", GradeDefinition.V4);
        createProblem(wall.getId(), "Pink", GradeDefinition.V3);
        createProblem(wall.getId(), "Teal", GradeDefinition.V5);

        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                wall.getId(), GradeDefinition.V4, GradeDefinition.V4);

        assertEquals(1, responses.size());
        assertEquals(onlyV4.problemId(), responses.get(0).problemId());
        assertEquals(GradeDefinition.V4, responses.get(0).assignedGrade());
    }

    @Test
    @DisplayName("Returns empty list when no active problems match the range")
    void returnsEmptyListWhenNoMatches() {
        WallSection wall = createWallSection("Empty Range Wall");
        createProblem(wall.getId(), "Black", GradeDefinition.V0);

        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                wall.getId(), GradeDefinition.V15, GradeDefinition.V17);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Throws 404 when wall section does not exist")
    void throwsNotFoundForMissingWallSection() {
        Long missingWallId = 999_999L;

        RuntimeException unsorted = assertThrows(RuntimeException.class, () ->
                problemFilteringService.findProblemsByRange(missingWallId, GradeDefinition.V0, GradeDefinition.V5));

        RuntimeException asc = assertThrows(RuntimeException.class, () ->
                problemFilteringService.findProblemBetweenRangeAsc(missingWallId, GradeDefinition.V0, GradeDefinition.V5));

        RuntimeException desc = assertThrows(RuntimeException.class, () ->
                problemFilteringService.findProblemBetweenRangeDesc(missingWallId, GradeDefinition.V0, GradeDefinition.V5));
    }

    @Test
    @DisplayName("Does not return problems belonging to another wall section")
    void doesNotLeakProblemsFromOtherWalls() {
        WallSection wallA = createWallSection("Wall A Isolation");
        WallSection wallB = createWallSection("Wall B Isolation");
        ClimbingProblemResponse problemA = createProblem(wallA.getId(), "Red", GradeDefinition.V2);
        ClimbingProblemResponse problemB = createProblem(wallB.getId(), "Blue", GradeDefinition.V2);

        List<ClimbingProblemResponse> responses = problemFilteringService.findProblemsByRange(
                wallA.getId(), GradeDefinition.V0, GradeDefinition.V5);

        Set<Long> ids = responses.stream().map(ClimbingProblemResponse::problemId).collect(Collectors.toSet());
        assertTrue(ids.contains(problemA.problemId()));
        assertFalse(ids.contains(problemB.problemId()));
    }

    @Test
    @DisplayName("Ascending and descending sort return the same set in opposite grade order")
    void ascendingAndDescendingReturnOppositeOrder() {
        WallSection wall = createWallSection("Sort Order Wall");
        createProblem(wall.getId(), "A", GradeDefinition.V1);
        createProblem(wall.getId(), "B", GradeDefinition.V3);
        createProblem(wall.getId(), "C", GradeDefinition.V6);

        List<ClimbingProblemResponse> asc = problemFilteringService.findProblemBetweenRangeAsc(
                wall.getId(), GradeDefinition.V1, GradeDefinition.V6);
        List<ClimbingProblemResponse> desc = problemFilteringService.findProblemBetweenRangeDesc(
                wall.getId(), GradeDefinition.V1, GradeDefinition.V6);

        assertEquals(3, asc.size());
        assertEquals(3, desc.size());
        assertTrue(checkForResponseOrderAsc(asc));
        assertTrue(checkForResponseOrderDesc(desc));
        assertEquals(
                gradesOf(asc),
                gradesOf(desc)
        );
        assertEquals(
                List.of(GradeDefinition.V1, GradeDefinition.V3, GradeDefinition.V6),
                asc.stream().map(ClimbingProblemResponse::assignedGrade).toList()
        );
        assertEquals(
                List.of(GradeDefinition.V6, GradeDefinition.V3, GradeDefinition.V1),
                desc.stream().map(ClimbingProblemResponse::assignedGrade).toList()
        );
    }

    private String searchRangePath(Long wallSectionId, GradeDefinition lowest, GradeDefinition highest) {
        return "/api/search/" + wallSectionId + "?min=" + lowest.name() + "&max=" + highest.name();
    }

    private String searchRangeSortPath(Long wallSectionId, GradeDefinition lowest, GradeDefinition highest, String sort) {
        return searchRangePath(wallSectionId, lowest, highest) + "&sort=" + sort;
    }

    @Test
    @DisplayName("GET /search range endpoint returns 200 with matching problems")
    void controllerReturnsOkForValidRange() throws Exception {
        WallSection wall = createWallSection("Controller Range Wall");
        createProblem(wall.getId(), "Black", GradeDefinition.V2);
        createProblem(wall.getId(), "Red", GradeDefinition.V4);

        mockMvc.perform(get(URI.create(searchRangePath(wall.getId(), GradeDefinition.V2, GradeDefinition.V4)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /search range&sort=asc endpoint returns ascending grades")
    void controllerReturnsAscendingSort() throws Exception {
        WallSection wall = createWallSection("Controller Asc Wall");
        createProblem(wall.getId(), "A", GradeDefinition.V5);
        createProblem(wall.getId(), "B", GradeDefinition.V1);

        mockMvc.perform(get(URI.create(searchRangeSortPath(
                        wall.getId(), GradeDefinition.V1, GradeDefinition.V5, "asc")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignedGrade").value("V1"))
                .andExpect(jsonPath("$[1].assignedGrade").value("V5"));
    }

    @Test
    @DisplayName("GET /search range&sort=desc endpoint returns descending grades")
    void controllerReturnsDescendingSort() throws Exception {
        WallSection wall = createWallSection("Controller Desc Wall");
        createProblem(wall.getId(), "A", GradeDefinition.V5);
        createProblem(wall.getId(), "B", GradeDefinition.V1);

        mockMvc.perform(get(URI.create(searchRangeSortPath(
                        wall.getId(), GradeDefinition.V1, GradeDefinition.V5, "desc")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignedGrade").value("V5"))
                .andExpect(jsonPath("$[1].assignedGrade").value("V1"));
    }

    @Test
    @DisplayName("GET /search returns 404 when lower grade is higher than upper grade")
    void controllerReturnsBadRequestForInvalidRange() throws Exception {
        mockMvc.perform(get(URI.create(searchRangePath(1L, GradeDefinition.V10, GradeDefinition.V2)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /search returns 404 when wall section is missing")
    void controllerReturnsNotFoundForMissingWall() throws Exception {
        mockMvc.perform(get(URI.create(searchRangePath(999_999L, GradeDefinition.V0, GradeDefinition.V5)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /search returns client error for invalid grade query value")
    void controllerReturnsClientErrorForInvalidGrade() throws Exception {
        mockMvc.perform(get(URI.create("/api/search/1?min=V99&max=V5"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
