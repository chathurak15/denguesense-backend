package com.zeylex.denguesense.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public final class ForecastWeek {

    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

    private ForecastWeek() {
    }

    public static LocalDate nextWeekStart() {
        return LocalDate.now(COLOMBO).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
    public static LocalDate targetWeekAfter(LocalDate lastDengueWeekStart) {
        if (lastDengueWeekStart == null) {
            throw new IllegalArgumentException("lastDengueWeekStart is required");
        }
        return lastDengueWeekStart.plusWeeks(1);
    }
}
