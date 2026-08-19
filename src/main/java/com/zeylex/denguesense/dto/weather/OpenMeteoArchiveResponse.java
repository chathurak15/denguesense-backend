package com.zeylex.denguesense.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoArchiveResponse(Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            List<LocalDate> time,
            @JsonProperty("temperature_2m_mean") List<Double> temperature2mMean,
            @JsonProperty("temperature_2m_max") List<Double> temperature2mMax,
            @JsonProperty("temperature_2m_min") List<Double> temperature2mMin,
            @JsonProperty("precipitation_sum") List<Double> precipitationSum,
            @JsonProperty("relative_humidity_2m_mean") List<Double> relativeHumidity2mMean
    ) {
    }
}
