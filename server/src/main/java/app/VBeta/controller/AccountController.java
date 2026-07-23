package app.VBeta.controller;

import app.VBeta.api.dto.account.AccountRequest;
import app.VBeta.api.dto.account.AccountResponse;
import app.VBeta.api.dto.account.AccountRoleChangeRequest;
import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.domain.model.actions.ActionDefinition;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * {@code AccountController} exposes account-facing endpoints for session bootstrap and role updates.
 * <p>
 * It maps Firebase-authenticated principals into account login/upsert operations and routes
 * privileged role-change requests through authorization checks.
 * <p>
 * Business logic is delegated to {@link AccountService}, while permission enforcement for
 * sensitive operations is delegated to {@link AuthorizationService}.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;
  private final AuthorizationService authorizationService;


  /**
   * Constructs a new {@code AccountController} with account and authorization dependencies.
   *
   * @param accountService service responsible for account lifecycle operations
   * @param authorizationService service used to enforce action-based authorization
   */

  public AccountController(
    AccountService accountService,
    AuthorizationService authorizationService
  ) {
    this.accountService = accountService;
    this.authorizationService = authorizationService;
  }

  /**
   * Creates or resolves an account session for the authenticated Firebase principal.
   * <p>
   * The endpoint trusts the UID from the verified security context and prefers token-claim email
   * over client-supplied email when available.
   *
   * @param body account request payload containing username and fallback email
   * @return normalized account response for the resolved account
   * @throws ResponseStatusException with {@link HttpStatus#UNAUTHORIZED} when authentication is missing or invalid
   */
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

  /**
   * Changes the role of a target user account.
   * <p>
   * Caller must be authorized for {@link ActionDefinition#CHANGE_ROLE} before the update is applied.
   *
   * @param userId identifier of the user account whose role will be updated
   * @param body payload containing the desired target role
   * @return updated account response after role change
   */
  @PatchMapping("/{userId}/role")
  public AccountResponse changeUserRole(
    @PathVariable Long userId,
    @Valid @RequestBody AccountRoleChangeRequest body
  ) {
    authorizationService.authorizeCurrentUser(ActionDefinition.CHANGE_ROLE);

    return accountService.changeUserRole(userId, body.roleType());
  }

  /**
   * Retrieves all user accounts.
   * <p>
   * Caller must be authorized for {@link ActionDefinition#VIEW_ACCOUNTS} to access this endpoint.
   *
   * @return list of all account responses
   */
  @GetMapping
  public List<AccountResponse> getAllAccounts() {
    authorizationService.authorizeCurrentUser(ActionDefinition.VIEW_ACCOUNTS);

    return accountService.getAllAccounts();
  }
}
