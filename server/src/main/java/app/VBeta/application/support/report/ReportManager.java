package app.VBeta.application.support.report;

import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.domain.model.report.*;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.*;
import com.google.firebase.internal.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code ReportManager} encapsulates persistence and validation rules for
 * {@link Report} entities.
 * <p>
 * It resolves typed report targets, maps category names to catalog rows, and
 * exposes duplicate checks used by {@link app.VBeta.application.ModerationService}.
 */
@Service
@Transactional
public class ReportManager {
    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final ClimbingProblemRepository climbingProblemRepository;
    private final WallSectionRepository wallSectionRepository;
    private final UserAccountRepository userAccountRepository;
    private final DiscussionRootRepository discussionRootRepository;

    /**
     * Constructs a new {@code ReportManager} with report and target repositories.
     *
     * @param reportRepository repository for report entities
     * @param reportCategoryRepository repository for report category lookups
     * @param climbingProblemRepository repository for climbing problem targets
     * @param wallSectionRepository repository for wall section targets
     * @param userAccountRepository repository for user-account targets
     * @param discussionRootRepository repository for discussion targets
     */
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

    /**
     * Finds a report by identifier.
     *
     * @param id report identifier
     * @return matching report
     * @throws RuntimeException when missing
     */
    public Report findById(@NonNull Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    /**
     * Persists report changes to storage.
     *
     * @param report report entity to save
     * @return saved report entity
     */
    public Report save(Report report) {
        return reportRepository.save(report);
    }

    /**
     * Creates and stores a new {@code OPEN} report for a typed target.
     *
     * @param reporter authenticated reporter account
     * @param reportRequest report creation payload
     * @return persisted report
     * @throws RuntimeException when the category
     *         or target does not exist, or when the reporter owns the discussion/user target
     */
    public Report createReport(UserAccount reporter, ReportRequest reportRequest) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setCategory(getReportCategory(reportRequest.reportCategoryName()));
        report.setTargetType(reportRequest.reportTargetType());
        setTarget(report, reportRequest, reporter);
        report.setReportReason(reportRequest.reportReason());
        return save(report);
    }

    /**
     * Returns whether the reporter already has a conflicting report on the same target.
     * <p>
     * A conflict is an {@code OPEN} report on the target, or any prior report with the
     * same category on that target.
     *
     * @param request incoming report payload
     * @param user reporter account
     * @return {@code true} when a duplicate exists
     */
    public boolean checkForDuplicateReport(ReportRequest request, UserAccount user){
        return checkForDuplicateOpenReport(request, user) ||
                checkForDuplicateReportCategory(request, user);
    }

    public List<Report> getActiveReports(UserAccount user){
        List<Report> reports = reportRepository.findAllByReportStatus(ReportStatus.OPEN);
        return reports.stream().filter(report ->
                !(report.getTargetType().equals(ReportTargetType.DISCUSSION) &&
                    report.getDiscussion().getUserAccount().equals(user)) &&
                        !(report.getTargetType().equals(ReportTargetType.USER_ACCOUNT) &&
                                report.getUser().equals(user))
        ).toList();
    }

    public List<Report> findOpenByTarget(Report report, UserAccount user){
        if (!(report.getTargetType().equals(ReportTargetType.USER_ACCOUNT) && report.getUser().equals(user))
        && !(report.getTargetType().equals(ReportTargetType.DISCUSSION) && report.getUser().equals(user))){
            return null;
        }

        return reportRepository.findAllByCategoryAndTarget(ReportStatus.OPEN, report.getCategory(),
            report.getWallSection(), report.getDiscussion(), report.getProblem(), report.getUser());
    }

    /**
     * Returns whether the reporter already used the same category on this target.
     *
     * @param request incoming report payload
     * @param user reporter account
     * @return {@code true} when a same-category report exists
     */
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

    /**
     * Returns whether the reporter already has an {@code OPEN} report on this target.
     *
     * @param request incoming report payload
     * @param user reporter account
     * @return {@code true} when an open report exists
     */
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

    /**
     * Resolves a report category catalog row by name.
     *
     * @param categoryName category enum from the request
     * @return matching category row
     * @throws RuntimeException when missing
     */
    private ReportCategory getReportCategory(ReportCategoryName categoryName) {
        return reportCategoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new RuntimeException("Report category not found"));
    }

    /**
     * Sets exactly one typed target FK from {@code request.targetId()}.
     *
     * @param report report being populated
     * @param request report creation payload
     * @param reporter reporter used to reject self-owned discussion and user targets
     * @throws RuntimeException when the target
     *         is missing, deleted, or owned by the reporter
     */
    private void setTarget(Report report, ReportRequest request, UserAccount reporter){
        Long targetId = request.targetId();
        switch(request.reportTargetType()){
            case CLIMBING_PROBLEM -> report.setProblem(
                climbingProblemRepository.findById(targetId).
                        orElseThrow(() -> new RuntimeException("Climbing problem not found")));

            case DISCUSSION -> report.setDiscussion(
                discussionRootRepository.findByDiscussionIdAndDeletedByIsNullAndNotFromUser(targetId, reporter)
                        .orElseThrow(() -> new RuntimeException("Discussion not found")));

            case USER_ACCOUNT -> report.setUser(
                userAccountRepository.findByIdAndNotFirebaseUid(targetId, reporter.getFirebaseUid())
                        .orElseThrow(() -> new RuntimeException("User account not found")));

            case WALL_SECTION -> report.setWallSection(
                wallSectionRepository.findById(targetId)
                        .orElseThrow(() ->  new RuntimeException("Wall section not found")));
        }
    }
}
