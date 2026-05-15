package app.VBeta;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.AccountRequest;
import app.VBeta.application.AccountService;
import app.VBeta.controller.AccountController;
import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountService accountService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }
    

    @Test
    void testSessionReturnsWhenValidationFails() throws Exception {
        String json =
                """
                {
                  "username": "testuser",
                  "email": " "
                }
                """;

        mockMvc.perform(
                        post("/api/accounts/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessionReturns500WhenManagerThrowsIllegalState() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("uid123", null, null));

        AccountRequest req = new AccountRequest("testuser", "testUser@gmail.com");

        when(accountService.loginAccount(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Test exception here"));

        Exception ex = assertThrows(
                Exception.class,
                () -> mockMvc.perform(
                        post("/api/accounts/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))));

        assertInstanceOf(ServletException.class, ex);
        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Test exception here", ex.getCause().getMessage());
    }

}
