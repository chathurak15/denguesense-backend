package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "weather_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_weather_district_week",
                columnNames = {"district_id", "week_start_date"}
        ),
        indexes = @Index(name = "idx_weather_district_date", columnList = "district_id, week_start_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRecord {

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

    @Column(name = "temp_mean")
    private Double tempMean;

    @Column(name = "temp_max")
    private Double tempMax;

    @Column(name = "temp_min")
    private Double tempMin;

    @Column(name = "rainfall_mm")
    private Double rainfallMm;

    @Column(name = "humidity_pct")
    private Double humidityPct;
}
