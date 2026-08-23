package com.zeylex.denguesense.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WeeklyWeatherAggregator — Open-Meteo daily to epi-week mapping")
class WeeklyWeatherAggregatorTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 17);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 23);

    @Test
    @DisplayName("matches fetch_weather.py: mean temp, max max, min min, sum rain, mean humidity")
    void aggregatesLikePythonScript() {
        List<DailyWeatherObservation> daily = List.of(
                day(WEEK_START.minusDays(1), 40.0, 50.0, 10.0, 99.0, 10.0),
                day(WEEK_START, 28.0, 31.0, 25.0, 2.0, 80.0),
                day(WEEK_START.plusDays(1), 29.0, 32.0, 26.0, 0.0, 75.0),
                day(WEEK_START.plusDays(2), 27.0, 30.0, 24.0, 10.0, 90.0),
                day(WEEK_END.plusDays(1), 10.0, 11.0, 9.0, 50.0, 20.0)
        );

        WeeklyWeatherStats stats = WeeklyWeatherAggregator.aggregate(daily, WEEK_START, WEEK_END);

        assertThat(stats.tempMean()).isEqualTo(28.0);
        assertThat(stats.tempMax()).isEqualTo(32.0);
        assertThat(stats.tempMin()).isEqualTo(24.0);
        assertThat(stats.rainfallMm()).isEqualTo(12.0);
        assertThat(stats.humidityPct()).isEqualTo((80.0 + 75.0 + 90.0) / 3.0);
    }

    @Test
    @DisplayName("skips null humidity in the mean, like pandas skipna")
    void skipsNullHumidity() {
        List<DailyWeatherObservation> daily = List.of(
                day(WEEK_START, 28.0, 31.0, 25.0, 1.0, 80.0),
                day(WEEK_START.plusDays(1), 30.0, 33.0, 27.0, 1.0, null)
        );

        WeeklyWeatherStats stats = WeeklyWeatherAggregator.aggregate(daily, WEEK_START, WEEK_END);

        assertThat(stats.humidityPct()).isEqualTo(80.0);
        assertThat(stats.tempMean()).isEqualTo(29.0);
        assertThat(stats.rainfallMm()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("throws when no daily rows fall inside the week window")
    void noDaysInWeek() {
        List<DailyWeatherObservation> daily = List.of(
                day(WEEK_START.minusDays(1), 28.0, 31.0, 25.0, 1.0, 80.0)
        );

        assertThatThrownBy(() -> WeeklyWeatherAggregator.aggregate(daily, WEEK_START, WEEK_END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No daily weather observations");
    }

    private static DailyWeatherObservation day(LocalDate date, Double tempMean, Double tempMax, Double tempMin,
                                               Double rain, Double humidity) {
        return new DailyWeatherObservation(date, tempMean, tempMax, tempMin, rain, humidity);
    }
}
