package app.VBeta.domain.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code EventType} is a catalog row for notifiable event kinds and descriptions.
 */
@Entity
@Table(name = "event_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_type_id")
    private Long eventTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type_name", length = 50, nullable = false, unique = true)
    private EventTypeName eventTypeName;

    @Column(name = "description", length = 100)
    private String description;
}
