package app.VBeta.repository;

import app.VBeta.domain.model.moderation.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link ModerationAction} logbook rows.
 */
public interface ModerationRepository extends JpaRepository<ModerationAction, Long> {
    @Query("Select ma From ModerationAction ma Order By ma.createdAt Desc Limit 25 Offset :offSetNum")
    List<ModerationAction> findAllByOrderByCreatedAtDesc(@Param("offSetNum") int offSetNum);
}
