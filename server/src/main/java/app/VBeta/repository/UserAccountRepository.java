package app.VBeta.repository;

import app.VBeta.domain.model.actions.GymRole;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Returns all accounts assigned the given gym role type.
     *
     * @param roleType role type
     * @return matching accounts
     */
    List<UserAccount> findByGymRole_RoleType(RoleType roleType);

    /**
     * Finds an account by id that is not the requester's Firebase UID.
     *
     * @param targetId account identifier
     * @param requesterId requester Firebase UID to exclude
     * @return matching account when present
     */
    @Query("SELECT ua from UserAccount  ua WHERE ua.id = :targetId AND ua.firebaseUid <> :requesterId")
    Optional<UserAccount> findByIdAndNotFirebaseUid(@Param("targetId") Long targetId,
                                                    @Param("requesterId") String requesterId);
}