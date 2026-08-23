package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.ReportStatusUpdateDTO;
import com.zeylex.denguesense.dto.requestDTO.ReportSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ReportService {
    ReportResponseDTO saveReport(String deviceUUID, ReportSubmitDTO dto, MultipartFile image);
    PaginatedDTO getAllReports(ReportStatus status, Long districtId, LandType landType, Pageable pageable);
    ReportResponseDTO getReportById(Long id);
    ReportResponseDTO updateReportStatus(Long id, ReportStatusUpdateDTO dto, String currentUserEmail);
    PaginatedDTO getMyReports(String deviceUUID, Pageable pageable);
    ReportResponseDTO getMyReportById(String deviceUUID, Long id);

    // ── PHI district-scoped views ─────────────────────────────────────────────
    PaginatedDTO getDistrictReports(String phiEmail, Pageable pageable);

    PaginatedDTO getDistrictResolvedReports(String phiEmail, Pageable pageable);

    PaginatedDTO getMyResolvedReports(String phiEmail, Pageable pageable);
}