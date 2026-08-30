package app.VBeta.repository;

import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link ModerationAction} logbook rows.
 */
public interface ModerationRepository extends JpaRepository<ModerationAction, Long> {
    /**
     * Returns up to 25 logbook rows ordered by {@code createdAt} descending,
     * skipping {@code offSetNum} rows.
     *
     * @param offSetNum SQL offset ({@code 0} for the newest page)
     * @return matching logbook rows
     */
    @Query("Select ma From ModerationAction ma Order By ma.createdAt Desc Limit 25 Offset :offSetNum")
    List<ModerationAction> findAllByOrderByCreatedAtDesc(@Param("offSetNum") int offSetNum);

    /**
     * Returns logbook rows for one report, oldest first.
     *
     * @param report report whose decisions to load
     * @return matching logbook rows
     */
    List<ModerationAction> findByReportOrderByCreatedAtAsc(Report report);
}
