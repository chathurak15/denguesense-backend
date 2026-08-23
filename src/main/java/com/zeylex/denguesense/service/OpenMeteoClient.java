package com.zeylex.denguesense.service;

import com.zeylex.denguesense.weather.DailyWeatherObservation;
import com.zeylex.denguesense.weather.LatLon;

import java.time.LocalDate;
import java.util.List;

public interface OpenMeteoClient {

    List<DailyWeatherObservation> fetchDaily(LatLon coordinates, LocalDate startDate, LocalDate endDate);
}
