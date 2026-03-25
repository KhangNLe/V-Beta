package edu.ics499.VBeta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final UserAccountManager userAccountManager;


    public AccountController(UserAccountManager userAccountManager) {
        this.userAccountManager = userAccountManager;
    }

    @PostMapping("/session")
    public AccountResponse session(@Valid @RequestBody AccountRequest body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication");
        }

        String verifiedUid = String.valueOf(auth.getPrincipal());

        if (!verifiedUid.equals(body.firebaseUid())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Firebase UID mismatch");
        }

        return userAccountManager.loginAccount(body.username(), body.email(), verifiedUid);
    }
}
