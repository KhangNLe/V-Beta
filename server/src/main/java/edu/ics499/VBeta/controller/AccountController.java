package edu.ics499.VBeta.controller;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.api.dto.AccountRoleChangeRequest;
import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.application.AuthorizationService;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;
  private final AuthorizationService authorizationService;

  public AccountController(
    AccountService accountService,
    AuthorizationService authorizationService
  ) {
    this.accountService = accountService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/session")
  public AccountResponse session(@Valid @RequestBody AccountRequest body) {
    Authentication auth = SecurityContextHolder
      .getContext()
      .getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "Missing or invalid authentication"
      );
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

    return accountService.loginAccount(
      body.username(),
      resolvedEmail,
      verifiedUid
    );
  }

  @PatchMapping("/{userId}/role")
  public AccountResponse changeUserRole(
    @PathVariable Long userId,
    @Valid @RequestBody AccountRoleChangeRequest body
  ) {
    authorizationService.authorizeCurrentUser(ActionDefinition.CHANGE_ROLE);

    return accountService.changeUserRole(userId, body.roleType());
  }
}
