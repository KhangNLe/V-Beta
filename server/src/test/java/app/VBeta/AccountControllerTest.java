package app.VBeta;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.VBeta.api.dto.account.AccountRequest;
import app.VBeta.api.dto.account.AccountRoleChangeRequest;
import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.controller.AccountController;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.actions.RoleType;
import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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

    @Autowired
    private AuthorizationService authorizationService;

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

    @Test
    @DisplayName("POST /api/accounts/session returns created account")
    void returns201_whenSessionSucceeds() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("uid123", null, null));

        AccountRequest req = new AccountRequest("climber01", "climber01@example.com");
        when(accountService.loginAccount("climber01", "climber01@example.com", "uid123"))
                .thenReturn(new UserAccountDTO(7L, "climber01", "climber01@example.com", "CLIMBER"));

        mockMvc.perform(post("/api/accounts/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.username").value("climber01"))
                .andExpect(jsonPath("$.role").value("CLIMBER"));
    }

    @Test
    @DisplayName("PATCH /api/accounts/{id}/role returns updated account")
    void returns200_whenChangingRole() throws Exception {
        doNothing().when(authorizationService).authorizeCurrentUser(ActionDefinition.CHANGE_ROLE);
        when(accountService.changeUserRole(7L, RoleType.SETTER))
                .thenReturn(new UserAccountDTO(7L, "climber01", "climber01@example.com", "SETTER"));

        mockMvc.perform(patch("/api/accounts/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountRoleChangeRequest(RoleType.SETTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SETTER"));

        verify(accountService, times(1)).changeUserRole(7L, RoleType.SETTER);
    }

    @Test
    @DisplayName("PATCH role maps authorization failure to 404")
    void returns404_whenRoleChangeUnauthorized() throws Exception {
        doThrow(new RuntimeException("Role CLIMBER is not allowed to perform action CHANGE_ROLE"))
                .when(authorizationService).authorizeCurrentUser(ActionDefinition.CHANGE_ROLE);

        mockMvc.perform(patch("/api/accounts/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountRoleChangeRequest(RoleType.SETTER))))
                .andExpect(status().isNotFound());

        verify(accountService, never()).changeUserRole(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GET /api/accounts returns account list")
    void returns200_whenListingAccounts() throws Exception {
        doNothing().when(authorizationService).authorizeCurrentUser(ActionDefinition.VIEW_ACCOUNTS);
        when(accountService.getAllAccounts()).thenReturn(java.util.List.of(
                new UserAccountDTO(7L, "climber01", "climber01@example.com", "CLIMBER")
        ));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(7))
                .andExpect(jsonPath("$[0].username").value("climber01"));
    }

    @Test
    @DisplayName("GET /api/accounts maps authorization failure to 404")
    void returns404_whenListingAccountsUnauthorized() throws Exception {
        doThrow(new RuntimeException("Role CLIMBER is not allowed to perform action VIEW_ACCOUNTS"))
                .when(authorizationService).authorizeCurrentUser(ActionDefinition.VIEW_ACCOUNTS);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isNotFound());

        verify(accountService, never()).getAllAccounts();
    }

}
