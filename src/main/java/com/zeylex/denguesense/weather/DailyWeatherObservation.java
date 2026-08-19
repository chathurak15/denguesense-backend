package com.zeylex.denguesense.weather;

import java.time.LocalDate;

public record DailyWeatherObservation(
        LocalDate date,
        Double tempMean,
        Double tempMax,
        Double tempMin,
        Double rainfallMm,
        Double humidityPct
) {
}
