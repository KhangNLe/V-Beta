package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * {@code AccountController} exposes session/account bootstrap endpoints.
 * <p>
 * It maps authenticated Firebase principal data into account login/upsert operations
 * handled by {@link AccountService}.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;


    /**
     * Constructs a new {@code AccountController} with account service dependency.
     *
     * @param accountService service responsible for account login/upsert behavior
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Creates or resolves a session account for the authenticated principal.
     * <p>
     * Email may be overridden by token claims when available.
     *
     * @param body account request payload from client
     * @return normalized account response
     */
    @PostMapping("/session")
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

        return accountService.loginAccount(body.username(), resolvedEmail, verifiedUid);
    }
}
