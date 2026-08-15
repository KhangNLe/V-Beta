package app.VBeta.application.support.report;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.domain.model.report.*;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.*;
import com.google.firebase.internal.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class ReportManager {
    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final ClimbingProblemRepository climbingProblemRepository;
    private final WallSectionRepository wallSectionRepository;
    private final UserAccountRepository userAccountRepository;
    private final DiscussionRootRepository discussionRootRepository;

    public ReportManager(ReportRepository reportRepository,
                         ReportCategoryRepository reportCategoryRepository,
                         ClimbingProblemRepository climbingProblemRepository,
                         WallSectionRepository wallSectionRepository,
                         UserAccountRepository userAccountRepository,
                         DiscussionRootRepository discussionRootRepository) {
        this.reportRepository = reportRepository;
        this.reportCategoryRepository = reportCategoryRepository;
        this.climbingProblemRepository = climbingProblemRepository;
        this.wallSectionRepository = wallSectionRepository;
        this.userAccountRepository = userAccountRepository;
        this.discussionRootRepository = discussionRootRepository;
    }

    public Report findById(@NonNull Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Report save(Report report) {
        return reportRepository.save(report);
    }

    public Report createReport(UserAccount reporter, ReportRequest reportRequest) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setCategory(getReportCategory(reportRequest.reportCategoryName()));
        report.setTargetType(reportRequest.reportTargetType());
        setTarget(report, reportRequest, reporter);
        report.setReportReason(reportRequest.reportReason());
        return save(report);
    }

    public boolean checkForDuplicateReport(ReportRequest request, UserAccount user){
        return checkForDuplicateOpenReport(request, user) ||
                checkForDuplicateReportCategory(request, user);
    }

    private boolean checkForDuplicateReportCategory(ReportRequest request, UserAccount user){
        ReportCategory reportCategory = getReportCategory(request.reportCategoryName());
        return switch(request.reportTargetType()) {
            case DISCUSSION -> reportRepository
                    .existsByReporterAndCategoryAndDiscussion_DiscussionId(user, reportCategory, request.targetId());
            case WALL_SECTION -> reportRepository
                    .existsByReporterAndCategoryAndWallSection_Id(user, reportCategory, request.targetId());
            case CLIMBING_PROBLEM -> reportRepository
                    .existsByReporterAndCategoryAndProblem_Id(user, reportCategory, request.targetId());
            case USER_ACCOUNT ->  reportRepository
                    .existsByReporterAndCategoryAndUser_Id(user, reportCategory, request.targetId());
        };
    }

    private boolean checkForDuplicateOpenReport(ReportRequest request, UserAccount user){
        return switch(request.reportTargetType()) {
            case DISCUSSION -> reportRepository
                    .existsByReporterAndReportStatusAndDiscussion_DiscussionId(user, ReportStatus.OPEN, request.targetId());
            case WALL_SECTION -> reportRepository
                    .existsByReporterAndReportStatusAndWallSection_Id(user, ReportStatus.OPEN, request.targetId());
            case CLIMBING_PROBLEM -> reportRepository
                    .existsByReporterAndReportStatusAndProblem_Id(user, ReportStatus.OPEN, request.targetId());
            case USER_ACCOUNT ->  reportRepository
                    .existsByReporterAndReportStatusAndUser_Id(user, ReportStatus.OPEN, request.targetId());
        };
    }

    private ReportCategory getReportCategory(ReportCategoryName categoryName) {
        return reportCategoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void setTarget(Report report, ReportRequest request, UserAccount reporter){
        Long targetId = request.targetId();
        switch(request.reportTargetType()){
            case CLIMBING_PROBLEM -> report.setProblem(
                climbingProblemRepository.findById(targetId).
                        orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case DISCUSSION -> report.setDiscussion(
                discussionRootRepository.findByDiscussionIdAndDeletedByIsNullAndNotFromUser(targetId, reporter)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case USER_ACCOUNT -> report.setUser(
                userAccountRepository.findByIdAndNotFirebaseUid(targetId, reporter.getFirebaseUid())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case WALL_SECTION -> report.setWallSection(
                wallSectionRepository.findById(targetId)
                        .orElseThrow(() ->  new ResponseStatusException(HttpStatus.NOT_FOUND)));
        }
    }
}
