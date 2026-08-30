package app.VBeta.repository;

import app.VBeta.domain.model.appeal.Appeal;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Appeal} rows.
 */
public interface AppealRepository extends JpaRepository<Appeal, Long> {
    /**
     * Returns the appeal for a report, if one exists.
     *
     * @param report report that may have been appealed
     * @return matching appeal, or empty when none
     */
    Optional<Appeal> findByReport(Report report);

    /**
     * Returns appeals in the given status, newest first.
     *
     * @param appealStatus status filter ({@code OPEN} for the admin queue)
     * @return matching appeals
     */
    List<Appeal> findByAppealStatusOrderByCreatedAtDesc(AppealStatus appealStatus);
}
