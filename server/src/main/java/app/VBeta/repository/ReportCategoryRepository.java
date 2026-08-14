package app.VBeta.repository;

import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.report.ReportCategoryName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportCategoryRepository extends JpaRepository<ReportCategory, Long> {
    Optional<ReportCategory> findByCategoryName(ReportCategoryName categoryName);
}
