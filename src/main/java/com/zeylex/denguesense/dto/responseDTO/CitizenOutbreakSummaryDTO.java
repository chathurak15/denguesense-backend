package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;
import java.util.List;

public record CitizenOutbreakSummaryDTO(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        int year,
        long lastWeekCases,
        long previousWeekCases,
        Double weekChangePercent,
        long yearCases,
        int hotspotCount,
        String nationalRisk,
        String banner,
        List<String> highDistricts
) {
}
