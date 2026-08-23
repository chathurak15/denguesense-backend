package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;

public record DengueCaseSummaryDTO(
        LocalDate lastWeekStartDate,
        LocalDate lastWeekEndDate,
        int lastWeekRdhsCount,
        int lastWeekRdhsExpected,
        long lastWeekCases,
        long lastWeekCumulativeTotal,
        int year,
        long nationalYearCases,
        boolean scopedToDistrict,
        Long districtId,
        String districtName,
        Long districtYearCumulative,
        Long districtYearCases,
        Long districtLastWeekCases
) {
    public static final int RDHS_EXPECTED = 26;
}
