package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "dengue_case_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cases_district_week",
                columnNames = {"district_id", "week_start_date"}
        ),
        indexes = @Index(name = "idx_cases_district_date", columnList = "district_id, week_start_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DengueCaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "week_cases")
    private Integer weekCases;

    @Column(name = "cumulative_cases")
    private Integer cumulativeCases;

    @Column(name = "week_cases_scaled")
    private Double weekCasesScaled;
}
