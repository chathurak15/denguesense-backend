package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.ForecastResultDTO;

import java.time.LocalDate;
import java.util.List;

public interface ForecastService {
    ForecastResultDTO generateForecast(Long districtId, LocalDate targetWeekStart);

    List<ForecastResultDTO> getForecastHistory(Long districtId);
}
