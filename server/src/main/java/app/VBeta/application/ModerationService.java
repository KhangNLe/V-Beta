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

@Service
@Transactional
public class ModerationService {
    private final ReportManager reportManager;
    private final UserAccountManager userAccountManager;
    private final NotificationService notificationService;

    public ModerationService(ReportManager reportManager,
                             UserAccountManager userAccountManager,
                             NotificationService notificationService) {
        this.reportManager = reportManager;
        this.userAccountManager = userAccountManager;
        this.notificationService = notificationService;
    }

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
