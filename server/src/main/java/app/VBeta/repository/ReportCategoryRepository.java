package app.VBeta.repository;

import app.VBeta.domain.model.report.ReportCategory;
import app.VBeta.domain.model.report.ReportCategoryName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link ReportCategory} entities.
 */
public interface ReportCategoryRepository extends JpaRepository<ReportCategory, Long> {
    /**
     * Finds a category row by seeded category name.
     *
     * @param categoryName category enum
     * @return matching category when present
     */
    Optional<ReportCategory> findByCategoryName(ReportCategoryName categoryName);
}
