package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
@Entity
@Table(
        name = "district_forecast",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_district_forecast_rdhs_week",
                columnNames = {"rdhs_id", "target_week_start"}
        ),
        indexes = @Index(name = "idx_district_forecast_rdhs_week", columnList = "rdhs_id, target_week_start")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rdhs_id", nullable = false)
    private Integer rdhsId;

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "target_week_start", nullable = false)
    private LocalDate targetWeekStart;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "predictions", nullable = false, columnDefinition = "double precision[]")
    private List<Double> predictions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "lower_bounds", nullable = false, columnDefinition = "double precision[]")
    private List<Double> lowerBounds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "upper_bounds", nullable = false, columnDefinition = "double precision[]")
    private List<Double> upperBounds;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ForecastStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", nullable = false)
    private GenerationSource generationSource;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Version
    private Long version;
}
