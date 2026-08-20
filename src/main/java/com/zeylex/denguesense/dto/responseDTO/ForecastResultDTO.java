package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;
import java.util.List;
public record ForecastResultDTO(
        String district,
        LocalDate forecastDate,
        String modelVersion,
        List<WeekPrediction> predictions
) {
    public record WeekPrediction(
            LocalDate targetDate,
            double predictedCases,
            Double lowerBound,
            Double upperBound
    ) {}
}
