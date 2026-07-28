package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.ResolutionRequestDTO;
import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;

public interface ResolutionService {

    ResolutionResponseDTO resolveReport(Long reportId, ResolutionRequestDTO dto, String phiEmail);

    ResolutionResponseDTO getResolutionByReportId(Long reportId);
    ResolutionResponseDTO getDistrictResolutionByReportId(Long reportId, String phiEmail);
}
