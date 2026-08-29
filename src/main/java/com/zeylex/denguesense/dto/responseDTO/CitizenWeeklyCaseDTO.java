package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDate;

public record CitizenWeeklyCaseDTO(
        LocalDate weekStartDate,
        Integer weekCases
) {
}
