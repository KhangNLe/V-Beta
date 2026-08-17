package app.VBeta.application.support.moderation;

import app.VBeta.api.dto.moderation.ModerationRequest;
import app.VBeta.domain.model.moderation.ModerateActionType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.ModerationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class ModerationManager {
    private final ModerationRepository moderationRepository;

    public ModerationManager(ModerationRepository moderationRepository) {
        this.moderationRepository = moderationRepository;
    }

    public Optional<ModerationAction> findById(Long id) {
        return moderationRepository.findById(id);
    }

    public ModerationAction save(ModerationAction moderationAction) {
        return moderationRepository.save(moderationAction);
    }

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
