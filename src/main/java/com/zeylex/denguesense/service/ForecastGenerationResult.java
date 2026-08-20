package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.DistrictForecast;

public record ForecastGenerationResult(Outcome outcome, DistrictForecast forecast, String message) {

    public enum Outcome {
        GENERATED,
        INCOMPLETE_HISTORY,
        UPSTREAM_TIMEOUT,
        UPSTREAM_ERROR,
        CONFLICT
    }

    public boolean isSuccess() {
        return outcome == Outcome.GENERATED;
    }

    public static ForecastGenerationResult generated(DistrictForecast forecast) {
        return new ForecastGenerationResult(Outcome.GENERATED, forecast, null);
    }

    public static ForecastGenerationResult incompleteHistory(DistrictForecast staleRowOrNull, String message) {
        return new ForecastGenerationResult(Outcome.INCOMPLETE_HISTORY, staleRowOrNull, message);
    }

    public static ForecastGenerationResult upstreamTimeout(String message) {
        return new ForecastGenerationResult(Outcome.UPSTREAM_TIMEOUT, null, message);
    }

    public static ForecastGenerationResult upstreamError(String message) {
        return new ForecastGenerationResult(Outcome.UPSTREAM_ERROR, null, message);
    }

    public static ForecastGenerationResult conflict(String message) {
        return new ForecastGenerationResult(Outcome.CONFLICT, null, message);
    }
}
