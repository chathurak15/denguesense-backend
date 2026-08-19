package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;

public record DengueCaseResponseDTO(
        Long id,
        Long districtId,
        String districtName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer weekCases,
        Integer cumulativeCases,
        boolean created,
        WeatherSummary weather,
        BsdsSummary bsds
) {
    public record WeatherSummary(
            Long id,
            Double tempMean,
            Double tempMax,
            Double tempMin,
            Double rainfallMm,
            Double humidityPct
    ) {
    }

    public record BsdsSummary(
            Long id,
            Double bsdsScore,
            Integer reportCount
    ) {
    }
}
