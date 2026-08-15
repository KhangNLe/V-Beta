package app.VBeta.api.dto.notification;

/**
 * Event-type summary included in a short notification DTO.
 *
 * @param eventTypeName catalog event type name
 * @param description human-readable event description
 */
public record EventTypeDTO (
    String eventTypeName,
    String description
 ) {}
