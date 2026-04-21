package edu.ics499.VBeta.api.dto;

import edu.ics499.VBeta.domain.model.RoleType;
import jakarta.validation.constraints.NotNull;

// This lets us use this class as a DTO for changing the role of an account. It only contains the new role type that we want to assign to the account. So the frontend can send a json object like { "roleType": "SETTER" }
public record AccountRoleChangeRequest (
  @NotNull RoleType roleType
  
 ) {}
