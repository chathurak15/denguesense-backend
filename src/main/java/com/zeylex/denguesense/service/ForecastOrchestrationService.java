package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.GenerationSource;

import java.time.LocalDate;

public interface ForecastOrchestrationService {
    ForecastGenerationResult generateForecast(Integer rdhsId, LocalDate targetWeekStart, GenerationSource source);

    ForecastGenerationResult generateForecast(Integer rdhsId, GenerationSource source);

    DistrictForecast getLatestForecast(Integer rdhsId);
}
