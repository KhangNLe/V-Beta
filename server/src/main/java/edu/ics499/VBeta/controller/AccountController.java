package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.UserAccountManager;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final UserAccountManager userAccountManager;


    public AccountController(UserAccountManager userAccountManager) {
        this.userAccountManager = userAccountManager;
    }

    @PostMapping("/session")
    @Transactional(readOnly = true)
    public AccountResponse session(@Valid @RequestBody AccountRequest body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication");
        }

        String verifiedUid = String.valueOf(auth.getPrincipal());

        String resolvedEmail = body.email();
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> claims) {
            Object emailClaim = claims.get("email");
            if (emailClaim instanceof String tokenEmail && !tokenEmail.isBlank()) {
                resolvedEmail = tokenEmail;
            }
        }

        return userAccountManager.loginAccount(body.username(), resolvedEmail, verifiedUid);
    }
}
