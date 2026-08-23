package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.weather.OpenMeteoArchiveResponse;
import com.zeylex.denguesense.exception.WeatherFetchException;
import com.zeylex.denguesense.service.OpenMeteoClient;
import com.zeylex.denguesense.weather.DailyWeatherObservation;
import com.zeylex.denguesense.weather.LatLon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OpenMeteoClientImpl implements OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClientImpl.class);
    private static final String DAILY_VARS = String.join(",",
            "temperature_2m_mean",
            "temperature_2m_max",
            "temperature_2m_min",
            "precipitation_sum",
            "relative_humidity_2m_mean");

    private final WebClient openMeteoWebClient;

    public OpenMeteoClientImpl(@Qualifier("openMeteoWebClient") WebClient openMeteoWebClient) {
        this.openMeteoWebClient = openMeteoWebClient;
    }

    @Override
    public List<DailyWeatherObservation> fetchDaily(LatLon coordinates, LocalDate startDate, LocalDate endDate) {
        OpenMeteoArchiveResponse response;
        try {
            response = openMeteoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/archive")
                            .queryParam("latitude", coordinates.latitude())
                            .queryParam("longitude", coordinates.longitude())
                            .queryParam("start_date", startDate)
                            .queryParam("end_date", endDate)
                            .queryParam("daily", DAILY_VARS)
                            .queryParam("timezone", "Asia/Colombo")
                            .build())
                    .retrieve()
                    .bodyToMono(OpenMeteoArchiveResponse.class)
                    .block();
        } catch (WebClientRequestException ex) {
            throw new WeatherFetchException("Open-Meteo archive API is unreachable: " + ex.getMessage(), ex);
        } catch (WebClientResponseException ex) {
            throw new WeatherFetchException(
                    "Open-Meteo archive API returned HTTP " + ex.getStatusCode()
                            + ": " + ex.getResponseBodyAsString(), ex);
        }

        if (response == null || response.daily() == null || response.daily().time() == null
                || response.daily().time().isEmpty()) {
            throw new WeatherFetchException(
                    "Open-Meteo returned no daily observations for " + startDate + " to " + endDate);
        }

        List<DailyWeatherObservation> days = mapDaily(response.daily());
        log.debug("Open-Meteo returned {} daily rows for ({}, {}) [{} to {}]",
                days.size(), coordinates.latitude(), coordinates.longitude(), startDate, endDate);
        return days;
    }

    private static List<DailyWeatherObservation> mapDaily(OpenMeteoArchiveResponse.Daily daily) {
        List<LocalDate> times = daily.time();
        List<DailyWeatherObservation> days = new ArrayList<>(times.size());
        for (int i = 0; i < times.size(); i++) {
            days.add(new DailyWeatherObservation(
                    times.get(i),
                    at(daily.temperature2mMean(), i),
                    at(daily.temperature2mMax(), i),
                    at(daily.temperature2mMin(), i),
                    at(daily.precipitationSum(), i),
                    at(daily.relativeHumidity2mMean(), i)
            ));
        }
        return Collections.unmodifiableList(days);
    }

    private static Double at(List<Double> values, int index) {
        if (values == null || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }
}
