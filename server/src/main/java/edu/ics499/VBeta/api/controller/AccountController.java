package edu.ics499.VBeta.api.controller;

import edu.ics499.VBeta.api.dto.AccountMeResponse;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("accountMeController")
@RequestMapping("/api")
public class AccountController {

    private final UserAccountRepository userAccountRepository;

    public AccountController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Returns the currently authenticated user's account info and role.
     * Authentication is established by FirebaseAuthFilter, which sets the Firebase UID as principal.
     * The response includes the account ID, username, email, and role (if any). If the account is not found, an exception is thrown.
     */
    @GetMapping("/account")
    public ResponseEntity<AccountMeResponse> currentAccount(Authentication authentication) {
        String firebaseUid = (String) authentication.getPrincipal();
        UserAccount account = userAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("Account not found for UID " + firebaseUid));

        // This will get the role name if the account has a gym role, otherwise it will be null in the response
        String role = account.getGymRole() != null ? account.getGymRole().getRoleType().name() : null;
        AccountMeResponse response = new AccountMeResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                role
        );
        return ResponseEntity.ok(response);
    }
}
