package edu.ics499.VBeta.api.dto;

public record CloudFileStorageRequest(
        String fileName,
        String contentType,
        Long problemId,
        Long wallSectionId
) {
}
