package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;

import java.time.LocalDate;

public interface ForecastFeatureAssemblyService {
    ForecastRequestDTO assembleFeatures(Long districtId, LocalDate targetWeekStart);
}
