package app.VBeta.application.support.report;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.domain.model.report.*;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.*;
import com.google.firebase.internal.NonNull;
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
        ReportCategory category = getReportCategory(request.reportCategoryName());
        List<Report> reports = reportRepository.findByReporterAndCategory(user, category);

        if (reports.isEmpty()) return false;

        boolean isIdentical = false;
        for (Report report : reports){
            switch (report.getTargetType()){
                case DISCUSSION -> isIdentical = Objects.equals(
                        report.getDiscussion().getDiscussionId(), request.targetId());
                case USER_ACCOUNT -> isIdentical = Objects.equals(
                        report.getUser().getId(), request.targetId());
                case WALL_SECTION -> isIdentical = Objects.equals(
                        report.getWallSection().getId(), request.targetId());
                case CLIMBING_PROBLEM -> isIdentical = Objects.equals(
                        report.getProblem().getId(), request.targetId());
            }
            isIdentical = isIdentical && Objects.equals(report.getReportStatus(), ReportStatus.OPEN);
            if (isIdentical) return true;
        }
        return false;
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
