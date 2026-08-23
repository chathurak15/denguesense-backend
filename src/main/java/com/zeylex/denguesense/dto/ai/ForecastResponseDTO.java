package com.zeylex.denguesense.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ForecastResponseDTO(
        @JsonProperty("predictions") List<Double> predictions,
        @JsonProperty("lower_bounds") List<Double> lowerBounds,
        @JsonProperty("upper_bounds") List<Double> upperBounds,
        @JsonProperty("model_version") String modelVersion
) {}
