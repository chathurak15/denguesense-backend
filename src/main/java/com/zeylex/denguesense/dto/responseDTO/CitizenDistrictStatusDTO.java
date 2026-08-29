package com.zeylex.denguesense.dto.responseDTO;

import java.util.List;

public record CitizenDistrictStatusDTO(
        Long districtId,
        String districtName,
        String province,
        Integer rdhsId,
        Double latitude,
        Double longitude,
        Long lastWeekCases,
        String risk,
        String trend,
        String summary,
        List<CitizenWeeklyCaseDTO> weeklyCases,
        DistrictForecastResponseDTO forecast
) {
}
