package app.VBeta.repository;

import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT rep FROM Report rep WHERE rep.reporter = :reporter AND rep.category = :category")
    List<Report> findByReporterAndCategory(@Param("reporter") UserAccount reporter,
                                               @Param("category") ReportCategory category);
}
