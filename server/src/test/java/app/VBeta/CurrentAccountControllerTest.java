package app.VBeta;

import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.controller.CurrentAccountController;
import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrentAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class CurrentAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private AccountService accountService;

    private UsernamePasswordAuthenticationToken firebaseAuth(String uid) {
        return new UsernamePasswordAuthenticationToken(uid, null, List.of());
    }

    @Test
    @DisplayName("GET /api/account returns current profile")
    void returns200_whenAccountExists() throws Exception {
        GymRole role = new GymRole();
        role.setRoleType(RoleType.CLIMBER);
        UserAccount account = new UserAccount();
        account.setId(7L);
        account.setUsername("climber01");
        account.setEmail("climber01@example.com");
        account.setFirebaseUid("testFirebaseUid");
        account.setGymRole(role);

        when(userAccountRepository.findByFirebaseUidWithRole("testFirebaseUid"))
                .thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/account").principal(firebaseAuth("testFirebaseUid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.username").value("climber01"))
                .andExpect(jsonPath("$.email").value("climber01@example.com"))
                .andExpect(jsonPath("$.role").value("CLIMBER"));
    }

    @Test
    @DisplayName("GET /api/account returns 404 when account is missing")
    void returns404_whenAccountDoesNotExist() throws Exception {
        when(userAccountRepository.findByFirebaseUidWithRole("missingUid"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/account").principal(firebaseAuth("missingUid")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/account returns 404 when authentication is missing")
    void returns404_whenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(userAccountRepository);
    }

    @Test
    @DisplayName("DELETE /api/account/deletion deletes the authenticated account")
    void returns200_whenDeletingCurrentAccount() throws Exception {
        when(authorizationService.getAuthenticatedFirebaseUid()).thenReturn("testFirebaseUid");
        doNothing().when(accountService).deleteAccount("testFirebaseUid");

        mockMvc.perform(delete("/api/account/deletion"))
                .andExpect(status().isOk());

        verify(accountService, times(1)).deleteAccount("testFirebaseUid");
    }
}
