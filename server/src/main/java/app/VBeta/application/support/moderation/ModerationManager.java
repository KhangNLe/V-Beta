package app.VBeta.application.support.moderation;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.ModerationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * {@code ModerationManager} persists append-only {@link ModerationAction} logbook
 * rows for admin report decisions.
 */
@Service
@Transactional
public class ModerationManager {
    private final ModerationRepository moderationRepository;

    /**
     * Constructs a new {@code ModerationManager} with moderation repository access.
     *
     * @param moderationRepository repository for logbook entities
     */
    public ModerationManager(ModerationRepository moderationRepository) {
        this.moderationRepository = moderationRepository;
    }

    /**
     * Finds a logbook row by identifier.
     *
     * @param id action identifier
     * @return matching action, or empty when missing
     */
    public Optional<ModerationAction> findById(Long id) {
        return moderationRepository.findById(id);
    }

    /**
     * Persists a logbook row.
     *
     * @param moderationAction action entity to save
     * @return saved action
     */
    public ModerationAction save(ModerationAction moderationAction) {
        return moderationRepository.save(moderationAction);
    }

    /**
     * Creates a logbook row for one report from a queue decision payload.
     *
     * @param report report being decided
     * @param user acting admin stored as {@code adminUser}
     * @param moderationRequest decision type and required notes
     * @return saved logbook row
     */
    public ModerationAction createModeration(Report report, UserAccount user, ModerationRequest moderationRequest) {
        return save(ModerationAction.builder()
                .report(report)
                .adminUser(user)
                .moderateActionType(moderationRequest.decision())
                .adminNotes(moderationRequest.reason())
                .build()
        );
    }
}
