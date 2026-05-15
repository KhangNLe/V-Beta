package app.VBeta.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite identifier for {@link RolePermission}, keyed by role and action IDs.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "action_id")
    private Long actionId;
}
