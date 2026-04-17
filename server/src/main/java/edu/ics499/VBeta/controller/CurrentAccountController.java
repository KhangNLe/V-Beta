package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.AccountMeResponse;
import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.domain.model.UserAccount;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CurrentAccountController {

    private final UserAccountRepository userAccountRepository;
    private final AuthorizationService authorizationService;
    private final AccountService accountService;

    public CurrentAccountController(UserAccountRepository userAccountRepository,
                                    AuthorizationService authorizationService,
                                    AccountService accountService) {
        this.userAccountRepository = userAccountRepository;
        this.authorizationService = authorizationService;
        this.accountService = accountService;
    }

    @GetMapping("/account")
    public ResponseEntity<AccountMeResponse> currentAccount(Authentication authentication) {
        String firebaseUid = (String) authentication.getPrincipal();
        UserAccount account = userAccountRepository.findByFirebaseUid(firebaseUid)
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

    @DeleteMapping("/account/deletion")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAccount(){
        String firebaseUid = authorizationService.getAuthenticatedFirebaseUid();
        accountService.deleteAccount(firebaseUid);
    }
}
