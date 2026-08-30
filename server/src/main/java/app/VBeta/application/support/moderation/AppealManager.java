package app.VBeta.application.support.moderation;

import app.VBeta.api.dto.moderation.AppealRequest;
import app.VBeta.domain.model.appeal.Appeal;
import app.VBeta.domain.model.appeal.AppealStatus;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.AppealRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * {@code AppealManager} persists one-time content-owner appeals after removal.
 */
@Service
@Transactional
public class AppealManager {
    private final AppealRepository appealRepository;

    /**
     * Constructs a new {@code AppealManager} with appeal repository access.
     *
     * @param appealRepository repository for appeal entities
     */
    public AppealManager(AppealRepository appealRepository) {
        this.appealRepository = appealRepository;
    }

    /**
     * Returns whether this report has no appeal yet.
     *
     * @param report report that may already have been appealed
     * @return {@code true} when no appeal row exists for {@code report}
     */
    public boolean isFirstAppeal(Report report) {
        return appealRepository.findByReport(report).isEmpty();
    }

    /**
     * Creates an {@code OPEN} appeal for the given report and owner.
     *
     * @param appealRequest owner-supplied reason
     * @param report removed report being appealed
     * @param appealUser content owner submitting the appeal
     * @return saved appeal
     */
    public Appeal createAppeal(AppealRequest appealRequest, Report report, UserAccount appealUser) {
        return save(Appeal.builder()
                .report(report)
                .appealUser(appealUser)
                .reason(appealRequest.appealReason())
                .build());
    }

    /**
     * Persists an appeal.
     *
     * @param appeal appeal entity to save
     * @return saved appeal
     */
    public Appeal save(Appeal appeal) {
        return appealRepository.save(appeal);
    }

    /**
     * Finds an appeal by identifier.
     *
     * @param appealId appeal identifier
     * @return matching appeal, or empty when missing
     */
    public Optional<Appeal> findById(Long appealId) {
        return appealRepository.findById(appealId);
    }

    /**
     * Returns {@code OPEN} appeals newest-first for the admin queue.
     *
     * @return matching appeals (empty when none are open)
     */
    public List<Appeal> findOpenAppeals() {
        return appealRepository.findByAppealStatusOrderByCreatedAtDesc(AppealStatus.OPEN);
    }

    /**
     * Finds the one-time appeal for a report, if any.
     *
     * @param report report that may have been appealed
     * @return matching appeal, or empty when none exists
     */
    public Optional<Appeal> findByReport(Report report) {
        return appealRepository.findByReport(report);
    }
}
