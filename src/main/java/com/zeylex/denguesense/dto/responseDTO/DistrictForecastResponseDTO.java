package com.zeylex.denguesense.dto.responseDTO;

import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DistrictForecastResponseDTO(
        Integer rdhsId,
        String districtName,
        LocalDate targetWeekStart,
        List<Double> predictions,
        List<Double> lowerBounds,
        List<Double> upperBounds,
        String modelVersion,
        ForecastStatus status,
        GenerationSource generationSource,
        Instant generatedAt
) {
    public static DistrictForecastResponseDTO from(DistrictForecast f) {
        return new DistrictForecastResponseDTO(
                f.getRdhsId(),
                f.getDistrictName(),
                f.getTargetWeekStart(),
                f.getPredictions(),
                f.getLowerBounds(),
                f.getUpperBounds(),
                f.getModelVersion(),
                f.getStatus(),
                f.getGenerationSource(),
                f.getGeneratedAt()
        );
    }
}
