package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.dto.ai.ForecastResponseDTO;


public interface ForecastApiClient {
    ForecastResponseDTO forecast(ForecastRequestDTO request);
}
