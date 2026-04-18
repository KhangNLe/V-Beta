package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.AccountMeResponse;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CurrentAccountController {

    private final UserAccountRepository userAccountRepository;

    public CurrentAccountController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/account")
    public ResponseEntity<AccountMeResponse> currentAccount(Authentication authentication) {
        String firebaseUid = (String) authentication.getPrincipal();
        UserAccount account = userAccountRepository.findByFirebaseUidWithRole(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("Account not found for UID " + firebaseUid));

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
