package edu.ics499.VBeta;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ics499.VBeta.api.dto.DiscussionCommentRequest;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.controller.ProblemDiscussionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = ProblemDiscussionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class TestProblemDiscussionController {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    private ProblemDiscussionService problemDiscussionService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void returns201_whenAuthenticated() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");

        DiscussionCommentRequest req = new DiscussionCommentRequest(2L, "Nice");
        mockMvc.perform(post("/discussion/add-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        verify(problemDiscussionService, times(1)).addComment(eq("testFirebaseUid"), any(DiscussionCommentRequest.class));
    }

    @Test
    void returns401_whenNoAuthentication() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid())
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token"));

        DiscussionCommentRequest req = new DiscussionCommentRequest(2L, "Nice");
        mockMvc.perform(post("/discussion/add-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
