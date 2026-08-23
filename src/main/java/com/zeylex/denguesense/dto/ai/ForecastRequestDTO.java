package com.zeylex.denguesense.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
public record ForecastRequestDTO(
        @JsonProperty("rdhs_id") int rdhsId,
        @JsonProperty("district_name") String districtName,
        @JsonProperty("target_week_start") LocalDate targetWeekStart,
        @JsonProperty("static_features") StaticFeatures staticFeatures,
        @JsonProperty("history") List<WeeklyRecord> history
) {

    public record StaticFeatures(
            @JsonProperty("zone_dry_zone") double zoneDryZone,
            @JsonProperty("zone_intermediate_zone") double zoneIntermediateZone,
            @JsonProperty("zone_wet_zone") double zoneWetZone,
            @JsonProperty("population_density") double populationDensity
    ) {}

    public record WeeklyRecord(
            @JsonProperty("week_no") Integer weekNo,
            @JsonProperty("week_start_date") LocalDate weekStartDate,
            @JsonProperty("week_end_date") LocalDate weekEndDate,
            @JsonProperty("temp_mean") Double tempMean,
            @JsonProperty("temp_max") Double tempMax,
            @JsonProperty("temp_min") Double tempMin,
            @JsonProperty("rainfall_mm") Double rainfallMm,
            @JsonProperty("humidity_pct") Double humidityPct,
            @JsonProperty("week_cases") Integer weekCases,
            @JsonProperty("cumulative_cases") Integer cumulativeCases
    ) {}
}
