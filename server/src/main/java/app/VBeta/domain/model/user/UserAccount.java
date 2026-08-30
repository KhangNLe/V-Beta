package app.VBeta.domain.model.user;

import app.VBeta.domain.model.actions.GymRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

/**
 * {@code UserAccount} represents an authenticated platform user and assigned gym role.
 */
@Entity
@Table(name = "User_Account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 25)
    private String username;

    @Column(name = "email", nullable = false, length = 225)
    private String email;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_role_id", referencedColumnName = "role_id")
    private GymRole gymRole;
}
