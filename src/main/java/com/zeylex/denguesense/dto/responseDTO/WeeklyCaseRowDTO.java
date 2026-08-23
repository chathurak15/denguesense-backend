package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;

public record WeeklyCaseRowDTO(
        Long id,
        Long districtId,
        String districtName,
        String rdhsZone,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Integer weekCases,
        Integer cumulativeCases
) {
}
