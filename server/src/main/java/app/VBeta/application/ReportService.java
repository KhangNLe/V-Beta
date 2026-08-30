package app.VBeta.application;

import app.VBeta.api.dto.problems.ClimbingProblemResponse;
import app.VBeta.api.dto.report.CategoryTallyDTO;
import app.VBeta.api.dto.report.ReportDTO;
import app.VBeta.api.dto.report.ReportPriorityDTO;
import app.VBeta.api.dto.report.ReportRequest;
import app.VBeta.api.dto.report.ReportUserDTO;
import app.VBeta.api.dto.report.ReportsPayload;
import app.VBeta.api.dto.walls.WallSectionResponse;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.report.ReportManager;
import app.VBeta.domain.model.actions.ActionDefinition;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.climb.WallSection;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportTargetType;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code ReportService} is the orchestration layer for content-report creation
 * and the admin report queue.
 * <p>
 * It resolves the reporter, enforces duplicate rules, persists the report through
 * {@link ReportManager}, asks {@link NotificationService} to notify admins, and
 * groups open reports into ranked queue cases.
 */
@Service
@Transactional
public class ReportService {
    private final ReportManager reportManager;
    private final UserAccountManager userAccountManager;
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;
    private final ClimbingProblemDiscussionManager climbingProblemDiscussionManager;
    private final ClimbingWallService climbingWallService;

    /**
     * Constructs a new {@code ReportService} with required collaborators.
     *
     * @param reportManager manager for report persistence and duplicate checks
     * @param userAccountManager manager for reporter account lookups
     * @param notificationService service for {@code REPORT_CREATED} admin inbox writes
     * @param authorizationService service for {@code VIEW_REPORTS} checks on queue/detail
     * @param climbingProblemDiscussionManager manager for discussion snapshots on queue cases
     * @param climbingWallService service for problem and wall snapshots on queue cases
     */
    public ReportService(ReportManager reportManager,
                         UserAccountManager userAccountManager,
                         NotificationService notificationService,
                         AuthorizationService authorizationService,
                         ClimbingProblemDiscussionManager climbingProblemDiscussionManager,
                         ClimbingWallService climbingWallService) {
        this.reportManager = reportManager;
        this.userAccountManager = userAccountManager;
        this.notificationService = notificationService;
        this.authorizationService = authorizationService;
        this.climbingProblemDiscussionManager = climbingProblemDiscussionManager;
        this.climbingWallService = climbingWallService;
    }

    /**
     * Creates an {@code OPEN} report for the authenticated user and notifies admins.
     *
     * @param reportRequest report target, category, and reason payload
     * @param firebaseUid Firebase UID of the reporter
     * @throws RuntimeException when the reporter
     *         account or target does not exist
     * @throws RuntimeException when a duplicate
     *         open report or same-category report already exists for the target
     */
    public void createNewReport(ReportRequest reportRequest, String firebaseUid) {
        UserAccount reporter = userAccountManager.findUserAccount(firebaseUid);
        if (reporter == null) {
            throw new RuntimeException("User not found");
        }

        if (reportManager.checkForDuplicateReport(reportRequest,  reporter)) {
            throw new RuntimeException("Report already exists");
        }

        Report report = reportManager.createReport(reporter, reportRequest);
        notificationService.saveNewReportNotification(report);
    }

    /**
     * Returns the OPEN case for the target of {@code reportId}.
     * <p>
     * Requires {@link ActionDefinition#VIEW_REPORTS}. Sibling OPEN reports on the
     * same target are grouped into one {@link ReportPriorityDTO}. If the viewer
     * owns the discussion or is the reported user, or no OPEN rows remain, the
     * payload {@code reports} list is empty.
     *
     * @param firebaseUid Firebase UID of the requesting admin
     * @param reportId identifier of any report on the target
     * @return one ranked case, or an empty list when hidden or fully dismissed
     * @throws RuntimeException when the account is missing, unauthorized, or
     *         {@code reportId} does not exist
     */
    public ReportsPayload getReport(String firebaseUid, Long reportId){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new  RuntimeException("User not found");
        }

        authorizationService.authorize(user, ActionDefinition.VIEW_REPORTS);
        Report report = reportManager.findById(reportId);
        List<Report> sameTarget = reportManager.findOpenByTarget(report, user);
        if (sameTarget.isEmpty()){
            return new ReportsPayload(new ArrayList<>());
        }
        return new ReportsPayload(List.of(toReportPriorityDTO(sameTarget)));
    }

    /**
     * Returns OPEN reports grouped by target and ranked by
     * {@code Σ (category weight × count)} descending.
     * <p>
     * Requires {@link ActionDefinition#VIEW_REPORTS}. Discussions owned by the
     * viewer and user-account reports targeting the viewer are omitted.
     *
     * @param firebaseUid Firebase UID of the requesting admin
     * @return ranked queue payload (empty list when nothing is visible)
     * @throws RuntimeException when the account is missing or unauthorized
     */
    public ReportsPayload getReportQueue(String firebaseUid){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        authorizationService.authorize(user, ActionDefinition.VIEW_REPORTS);
        List<Report> reports = reportManager.getActiveReports(user);
        return new ReportsPayload(createReportPriorityDTOs(reports));
    }

    private List<ReportPriorityDTO> createReportPriorityDTOs(List<Report> reports) {
        Map<TargetKey, List<Report>> byTarget = reports.stream()
                .collect(Collectors.groupingBy(this::targetKey, LinkedHashMap::new, Collectors.toList()));

        return byTarget.values().stream()
                .map(this::toReportPriorityDTO)
                .sorted(Comparator.comparingInt(ReportPriorityDTO::queueScore).reversed())
                .toList();
    }

    private ReportPriorityDTO toReportPriorityDTO(List<Report> reportsOnTarget) {
        ReportDTO report = toReportDTO(reportsOnTarget);
        List<CategoryTallyDTO> categories = toCategoryTallies(reportsOnTarget);
        int queueScore = categories.stream()
                .mapToInt(CategoryTallyDTO::categoryScore)
                .sum();
        return new ReportPriorityDTO(report, categories, queueScore);
    }

    /**
     * Maps one or more reports on the same target into a {@link ReportDTO}.
     * Logbook reads pass a single-report list (the decided reporter row).
     *
     * @param reportsOnTarget reports sharing one typed target
     * @return target snapshot plus {@code reporters}
     */
    public ReportDTO toReportDTO(List<Report> reportsOnTarget) {
        Report first = reportsOnTarget.get(0);
        List<ReportUserDTO> reporters = reportsOnTarget.stream()
                .map(this::toReportUserDTO)
                .toList();
        return new ReportDTO(
                first.getTargetType(),
                first.getTargetType() == ReportTargetType.DISCUSSION
                        ? climbingProblemDiscussionManager.getDiscussionData(first.getDiscussion())
                        : null,
                problemSnapshot(first),
                wallSnapshot(first),
                first.getTargetType() == ReportTargetType.USER_ACCOUNT
                        ? userAccountManager.getUserAccountDTO(first.getUser())
                        : null,
                reporters
        );
    }

    private ClimbingProblemResponse problemSnapshot(Report report) {
        ClimbingProblem problem = switch (report.getTargetType()) {
            case DISCUSSION -> {
                DiscussionRoot discussion = report.getDiscussion();
                yield discussion == null ? null : discussion.getProblem();
            }
            case CLIMBING_PROBLEM -> report.getProblem();
            default -> null;
        };
        return problem == null ? null : climbingWallService.getClimbingProblemResponse(problem);
    }

    private WallSectionResponse wallSnapshot(Report report) {
        WallSection wall = switch (report.getTargetType()) {
            case DISCUSSION -> {
                DiscussionRoot discussion = report.getDiscussion();
                ClimbingProblem problem = discussion == null ? null : discussion.getProblem();
                yield problem == null ? null : problem.getWallSection();
            }
            case CLIMBING_PROBLEM -> {
                ClimbingProblem problem = report.getProblem();
                yield problem == null ? null : problem.getWallSection();
            }
            case WALL_SECTION -> report.getWallSection();
            default -> null;
        };
        return wall == null ? null : climbingWallService.getWallSectionResponse(wall);
    }

    private ReportUserDTO toReportUserDTO(Report report) {
        return new ReportUserDTO(
                report.getReportId(),
                userAccountManager.getUserAccountDTO(report.getReporter()),
                report.getCategory().getCategoryName(),
                report.getReportReason(),
                report.getCreatedAt()
        );
    }

    private List<CategoryTallyDTO> toCategoryTallies(List<Report> reportsOnTarget) {
        return reportsOnTarget.stream()
                .collect(Collectors.groupingBy(
                        report -> report.getCategory().getCategoryName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(group -> {
                    int count = group.size();
                    int weight = group.get(0).getCategory().getWeight();
                    return new CategoryTallyDTO(
                            group.get(0).getCategory().getCategoryName(),
                            count,
                            weight * count
                    );
                })
                .toList();
    }

    private record TargetKey(ReportTargetType reportTargetType, Long targetId) {}

    private TargetKey targetKey(Report report) {
        Long targetId = switch (report.getTargetType()) {
            case CLIMBING_PROBLEM -> report.getProblem().getId();
            case WALL_SECTION -> report.getWallSection().getId();
            case DISCUSSION -> report.getDiscussion().getDiscussionId();
            case USER_ACCOUNT -> report.getUser().getId();
        };
        return new TargetKey(report.getTargetType(), targetId);
    }
}
