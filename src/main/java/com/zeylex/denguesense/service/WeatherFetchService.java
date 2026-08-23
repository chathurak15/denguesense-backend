package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.weather.DistrictWeekWindow;
import com.zeylex.denguesense.weather.WeeklyWeatherStats;

import java.time.LocalDate;
import java.util.List;

public interface WeatherFetchService {

    WeeklyWeatherStats fetchWeeklyAggregates(District district, LocalDate weekStartDate, LocalDate weekEndDate);

    WeatherRecord upsertWeeklyWeather(District district, LocalDate weekStartDate, LocalDate weekEndDate);

    int upsertWeeklyWeather(List<DistrictWeekWindow> weeks, List<String> errors);
}
