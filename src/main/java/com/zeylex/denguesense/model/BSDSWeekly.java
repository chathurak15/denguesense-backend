package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "bsds_weekly",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bsds_district_week",
                columnNames = {"district_id", "week_start_date"}
        ),
        indexes = @Index(name = "idx_bsds_district_date", columnList = "district_id, week_start_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BSDSWeekly {

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

    @Column(name = "bsds_score", nullable = false)
    private Double bsdsScore;

    @Column(name = "report_count")
    private Integer reportCount;
}
