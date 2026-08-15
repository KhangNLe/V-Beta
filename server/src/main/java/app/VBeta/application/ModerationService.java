package app.VBeta.application;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@code ModerationService} is the orchestration layer for content-report creation.
 * <p>
 * It resolves the reporter, enforces duplicate rules, persists the report through
 * {@link ReportManager}, and asks {@link NotificationService} to notify admins.
 */
@Service
@Transactional
public class ModerationService {
    private final ReportManager reportManager;
    private final UserAccountManager userAccountManager;
    private final NotificationService notificationService;

    /**
     * Constructs a new {@code ModerationService} with required collaborators.
     *
     * @param reportManager manager for report persistence and duplicate checks
     * @param userAccountManager manager for reporter account lookups
     * @param notificationService service for {@code REPORT_CREATED} admin inbox writes
     */
    public ModerationService(ReportManager reportManager,
                             UserAccountManager userAccountManager,
                             NotificationService notificationService) {
        this.reportManager = reportManager;
        this.userAccountManager = userAccountManager;
        this.notificationService = notificationService;
    }

    /**
     * Creates an {@code OPEN} report for the authenticated user and notifies admins.
     *
     * @param reportRequest report target, category, and reason payload
     * @param firebaseUid Firebase UID of the reporter
     * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when the reporter
     *         account or target does not exist
     * @throws ResponseStatusException with {@link HttpStatus#CONFLICT} when a duplicate
     *         open report or same-category report already exists for the target
     */
    public void createNewReport(ReportRequest reportRequest, String firebaseUid) {
        UserAccount reporter = userAccountManager.findUserAccount(firebaseUid);
        if (reporter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (reportManager.checkForDuplicateReport(reportRequest,  reporter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already exists");
        }

        Report report = reportManager.createReport(reporter, reportRequest);
        notificationService.saveReportNotification(report);
    }
}
