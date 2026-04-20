package edu.ics499.VBeta.api.dto;

/**
 * Lightweight request or reference DTO for selecting a wall section.
 *
 * @param wallSectionID unique identifier of the wall section
 * @param wallSectionName name of the wall section
 */
public record WallSectionRequest(
        Long wallSectionID,
        String wallSectionName
) {}
