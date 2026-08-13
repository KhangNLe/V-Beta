package app.VBeta.application.support.report;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.DiscussionRootManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.application.support.wall.WallSectionManager;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.report.ReportCategoryName;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.*;
import com.google.firebase.internal.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
        setTarget(report, reportRequest);
        report.setReportReason(reportRequest.reportReason());
        return save(report);
    }

    private ReportCategory getReportCategory(ReportCategoryName categoryName) {
        return reportCategoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void setTarget(Report report, ReportRequest request){
        Long targetId = request.targetId();
        switch(request.reportTargetType()){
            case CLIMBING_PROBLEM -> report.setProblem(
                climbingProblemRepository.findById(targetId).
                        orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case DISCUSSION -> report.setDiscussion(
                discussionRootRepository.findById(targetId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case USER_ACCOUNT -> report.setUser(
                userAccountRepository.findById(targetId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

            case WALL_SECTION -> report.setWallSection(
                wallSectionRepository.findById(targetId)
                        .orElseThrow(() ->  new ResponseStatusException(HttpStatus.NOT_FOUND)));
        }
    }
}
