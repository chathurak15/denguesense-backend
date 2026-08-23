package com.zeylex.denguesense.weather;

public record WeeklyWeatherStats(
        Double tempMean,
        Double tempMax,
        Double tempMin,
        Double rainfallMm,
        Double humidityPct
) {
}
