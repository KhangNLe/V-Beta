package app.VBeta.controller;

import app.VBeta.api.dto.account.UserAccountDTO;
import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * {@code CurrentAccountController} exposes endpoints for the currently authenticated account.
 * <p>
 * It provides account profile retrieval and self-service account deletion using the
 * Firebase-authenticated identity from the active security context.
 */
@RestController
@RequestMapping("/api")
public class CurrentAccountController {

    private final UserAccountRepository userAccountRepository;
    private final AuthorizationService authorizationService;
    private final AccountService accountService;

    /**
     * Constructs a new {@code CurrentAccountController} with account and authorization services.
     *
     * @param userAccountRepository repository for reading current account with role data
     * @param authorizationService authorization helper for current-user identity lookup
     * @param accountService service handling account-level operations
     */
    public CurrentAccountController(UserAccountRepository userAccountRepository,
                                    AuthorizationService authorizationService,
                                    AccountService accountService) {
        this.userAccountRepository = userAccountRepository;
        this.authorizationService = authorizationService;
        this.accountService = accountService;
    }

    /**
     * Returns profile information for the currently authenticated account.
     *
     * @param authentication Spring Security authentication object for current request
     * @return current account payload including role information
     */
    @GetMapping("/account")
    public ResponseEntity<?> currentAccount(Authentication authentication) {
        try {
            String firebaseUid = (String) authentication.getPrincipal();
            UserAccount account = userAccountRepository.findByFirebaseUidWithRole(firebaseUid)
                    .orElseThrow(() -> new IllegalStateException("Account not found for UID " + firebaseUid));

            String role = account.getGymRole() != null ? account.getGymRole().getRoleType().name() : null;
            UserAccountDTO response = new UserAccountDTO(
                    account.getId(),
                    account.getUsername(),
                    account.getEmail(),
                    role
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e){
            return new  ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes the currently authenticated account.
     */
    @DeleteMapping("/account/deletion")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAccount(){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        accountService.deleteAccount(firebaseUid);
    }
}
