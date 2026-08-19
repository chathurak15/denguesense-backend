package com.zeylex.denguesense.weather;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
public final class WeeklyWeatherAggregator {

    private WeeklyWeatherAggregator() {
    }

    public static WeeklyWeatherStats aggregate(
            List<DailyWeatherObservation> daily,
            LocalDate weekStartDate,
            LocalDate weekEndDate) {
        if (weekStartDate == null || weekEndDate == null) {
            throw new IllegalArgumentException("week start and end dates are required");
        }
        if (weekEndDate.isBefore(weekStartDate)) {
            throw new IllegalArgumentException("weekEndDate must be on or after weekStartDate");
        }
        if (daily == null || daily.isEmpty()) {
            throw new IllegalArgumentException(
                    "No daily weather observations between " + weekStartDate + " and " + weekEndDate);
        }

        List<DailyWeatherObservation> inWeek = new ArrayList<>();
        for (DailyWeatherObservation day : daily) {
            if (day == null || day.date() == null) {
                continue;
            }
            if (!day.date().isBefore(weekStartDate) && !day.date().isAfter(weekEndDate)) {
                inWeek.add(day);
            }
        }
        if (inWeek.isEmpty()) {
            throw new IllegalArgumentException(
                    "No daily weather observations between " + weekStartDate + " and " + weekEndDate);
        }

        return new WeeklyWeatherStats(
                mean(inWeek, DailyWeatherObservation::tempMean),
                max(inWeek, DailyWeatherObservation::tempMax),
                min(inWeek, DailyWeatherObservation::tempMin),
                sum(inWeek, DailyWeatherObservation::rainfallMm),
                mean(inWeek, DailyWeatherObservation::humidityPct)
        );
    }

    private static Double mean(List<DailyWeatherObservation> days,
                               Function<DailyWeatherObservation, Double> getter) {
        double total = 0.0;
        int count = 0;
        for (DailyWeatherObservation day : days) {
            Double value = finite(getter.apply(day));
            if (value != null) {
                total += value;
                count++;
            }
        }
        return count == 0 ? null : total / count;
    }

    private static Double max(List<DailyWeatherObservation> days,
                              Function<DailyWeatherObservation, Double> getter) {
        Double best = null;
        for (DailyWeatherObservation day : days) {
            Double value = finite(getter.apply(day));
            if (value != null && (best == null || value > best)) {
                best = value;
            }
        }
        return best;
    }

    private static Double min(List<DailyWeatherObservation> days,
                              Function<DailyWeatherObservation, Double> getter) {
        Double best = null;
        for (DailyWeatherObservation day : days) {
            Double value = finite(getter.apply(day));
            if (value != null && (best == null || value < best)) {
                best = value;
            }
        }
        return best;
    }

    private static Double sum(List<DailyWeatherObservation> days,
                              Function<DailyWeatherObservation, Double> getter) {
        double total = 0.0;
        boolean any = false;
        for (DailyWeatherObservation day : days) {
            Double value = finite(getter.apply(day));
            if (value != null) {
                total += value;
                any = true;
            }
        }
        return any ? total : 0.0;
    }

    private static Double finite(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return value;
    }
}
