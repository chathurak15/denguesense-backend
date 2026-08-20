package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.DengueCaseSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface WeeklyCaseQueryService {

    PaginatedDTO listWeeklyCases(
            String userEmail,
            Long districtId,
            String district,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    DengueCaseSummaryDTO getSummary(String userEmail);
}
