package app.VBeta.repository;

import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link Report} entities.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {
    /**
     * Returns whether the reporter has a report in the given status on a discussion.
     *
     * @param reporter reporter account
     * @param status report status
     * @param discussionId discussion identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndReportStatusAndDiscussion_DiscussionId(
            UserAccount reporter, ReportStatus status, Long discussionId);

    /**
     * Returns whether the reporter has a report in the given status on a user account.
     *
     * @param reporter reporter account
     * @param status report status
     * @param userId reported user identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndReportStatusAndUser_Id(
            UserAccount reporter, ReportStatus status, Long userId);

    /**
     * Returns whether the reporter has a report in the given status on a wall section.
     *
     * @param reporter reporter account
     * @param status report status
     * @param wallSectionId wall section identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndReportStatusAndWallSection_Id(
            UserAccount reporter, ReportStatus status, Long wallSectionId);

    /**
     * Returns whether the reporter has a report in the given status on a climbing problem.
     *
     * @param reporter reporter account
     * @param status report status
     * @param climbingProblemId climbing problem identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndReportStatusAndProblem_Id(
            UserAccount reporter, ReportStatus status, Long climbingProblemId
    );

    /**
     * Returns whether the reporter already used the given category on a user account.
     *
     * @param reporter reporter account
     * @param category report category
     * @param userId reported user identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndCategoryAndUser_Id(
            UserAccount reporter, ReportCategory category, Long userId
    );

    /**
     * Returns whether the reporter already used the given category on a wall section.
     *
     * @param reporter reporter account
     * @param category report category
     * @param wallSectionId wall section identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndCategoryAndWallSection_Id(
            UserAccount reporter, ReportCategory category, Long wallSectionId
    );

    /**
     * Returns whether the reporter already used the given category on a climbing problem.
     *
     * @param reporter reporter account
     * @param category report category
     * @param climbingProblemId climbing problem identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndCategoryAndProblem_Id(
            UserAccount reporter, ReportCategory category, Long climbingProblemId
    );

    /**
     * Returns whether the reporter already used the given category on a discussion.
     *
     * @param reporter reporter account
     * @param category report category
     * @param discussionId discussion identifier
     * @return {@code true} when a matching report exists
     */
    boolean existsByReporterAndCategoryAndDiscussion_DiscussionId(
            UserAccount reporter, ReportCategory category, Long discussionId
    );

    @Query("SELECT re FROM Report re WHERE re.reportStatus = :status")
    List<Report> findAllByReportStatus(@Param("status") ReportStatus status);

    List<Report> findAllByReportStatusAndDiscussion_DiscussionId(ReportStatus status, Long discussionId);

    List<Report> findAllByReportStatusAndProblem_Id(ReportStatus status, Long problemId);

    List<Report> findAllByReportStatusAndWallSection_Id(ReportStatus status, Long wallSectionId);

    List<Report> findAllByReportStatusAndUser_Id(ReportStatus status, Long userId);
}
