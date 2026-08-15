package app.VBeta.repository;

import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.report.ReportStatus;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.UserDetailsAwareConfigurer;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterAndReportStatusAndDiscussion_DiscussionId(
            UserAccount reporter, ReportStatus status, Long discussionId);

    boolean existsByReporterAndReportStatusAndUser_Id(
            UserAccount reporter, ReportStatus status, Long userId);

    boolean existsByReporterAndReportStatusAndWallSection_Id(
            UserAccount reporter, ReportStatus status, Long wallSectionId);

    boolean existsByReporterAndReportStatusAndProblem_Id(
            UserAccount reporter, ReportStatus status, Long climbingProblemId
    );

    boolean existsByReporterAndCategoryAndUser_Id(
            UserAccount reporter, ReportCategory category, Long userId
    );

    boolean existsByReporterAndCategoryAndWallSection_Id(
            UserAccount reporter, ReportCategory category, Long wallSectionId
    );

    boolean existsByReporterAndCategoryAndProblem_Id(
            UserAccount reporter, ReportCategory category, Long climbingProblemId
    );

    boolean existsByReporterAndCategoryAndDiscussion_DiscussionId(
            UserAccount reporter, ReportCategory category, Long discussionId
    );

}
