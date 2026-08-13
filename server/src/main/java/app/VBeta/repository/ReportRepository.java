package app.VBeta.repository;

import app.VBeta.domain.model.report.Report;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findById(@NonNull Long id);
}
