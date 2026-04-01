package edu.ics499.VBeta.api.dto;

public record AccountResponse(
        Long id,
        String username,
        String email,
        String firebaseUid,
        String roleName) {}
