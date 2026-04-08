package edu.ics499.VBeta.api.dto;

public record CloudFileStorageResponse(
        String signedURL,
        String method,
        String uploadObjectName,
        String publicURL
) {
}
