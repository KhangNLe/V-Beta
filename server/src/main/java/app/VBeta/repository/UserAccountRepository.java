package app.VBeta.repository;

import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

/**
 * Repository for {@link UserAccount} entities.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long>{
    /**
     * Finds an account by username.
     *
     * @param username username value
     * @return account when present
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Finds an account by email address.
     *
     * @param email email value
     * @return account when present
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * Finds an account by Firebase UID.
     *
     * @param firebaseUid Firebase UID value
     * @return account when present
     */
    Optional<UserAccount> findByFirebaseUid(String firebaseUid);

    /**
     * Finds an account by Firebase UID and eagerly fetches the associated role.
     *
     * @param firebaseUid Firebase UID value
     * @return account with role when present
     */
    @Query("select ua from UserAccount ua left join fetch ua.gymRole where ua.firebaseUid = :firebaseUid")
    Optional<UserAccount> findByFirebaseUidWithRole(String firebaseUid);
}