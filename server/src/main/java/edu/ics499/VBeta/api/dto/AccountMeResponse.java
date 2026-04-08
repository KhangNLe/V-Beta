package edu.ics499.VBeta.api.dto;

// DTO (Data Transfer Object) for the /api/account endpoint response, which includes the user's account info and role (if any).
public record AccountMeResponse(Long userId, String username, String email, String role) {}
