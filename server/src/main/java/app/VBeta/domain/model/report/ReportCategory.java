package app.VBeta.domain.model.report;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate. annotations.JdbcTypeCode;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "Report_Category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", length = 40)
    @Enumerated(EnumType.STRING)
    private ReportCategoryName categoryName;

    @Column(name = "priority", nullable = false, unique = true)
    private Integer priority;
}
