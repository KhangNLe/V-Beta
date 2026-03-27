package edu.ics499.VBeta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.UserAccountManager;
import edu.ics499.VBeta.controller.AccountController;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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
    private UserAccountManager userAccountManager;

    @Autowired
    private UserAccountRepository accountRepository;

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
                  "email": "testUser@gmail.com",
                  "firebaseUid": " "
                }
                """;

        mockMvc.perform(
                        post("/api/accounts/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Disabled
    @Test
    void sessionReturns500WhenManagerThrowsIllegalState() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("uid123", null, null));

        AccountRequest req = new AccountRequest("testuser", "testUser@gmail.com", "uid123");

        when(userAccountManager.loginAccount(any(AccountRequest.class)))
                .thenThrow(new IllegalStateException("Test exception here"));

        mockMvc.perform(
                        post("/api/accounts/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

}
