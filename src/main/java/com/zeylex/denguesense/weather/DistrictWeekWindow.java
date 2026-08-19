package com.zeylex.denguesense.weather;

import com.zeylex.denguesense.model.District;

import java.time.LocalDate;

public record DistrictWeekWindow(District district, LocalDate weekStartDate, LocalDate weekEndDate) {
}
