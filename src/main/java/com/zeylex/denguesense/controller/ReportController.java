package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.requestDTO.ReportStatusUpdateDTO;
import com.zeylex.denguesense.dto.requestDTO.ReportSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.service.ReportService;
import com.zeylex.denguesense.service.ResolutionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/reports")
@CrossOrigin
public class ReportController {

    private final ReportService reportService;
    private final ResolutionService resolutionService;

    public ReportController(ReportService reportService, ResolutionService resolutionService) {
        this.reportService = reportService;
        this.resolutionService = resolutionService;
    }

    // Anonymous citizen submission — multipart/form-data(image file + scalar form fields)
    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<ReportResponseDTO> saveReport(
            @RequestHeader("X-Device-UUID") String deviceUUID,
            @Valid @ModelAttribute ReportSubmitDTO dto,
            @RequestPart("image") MultipartFile image) {

        validateDeviceUUID(deviceUUID);
        ReportResponseDTO response = reportService.saveReport(deviceUUID, dto, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Authenticated staff only - paginated, optional filters
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<PaginatedDTO> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) LandType landType,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        PaginatedDTO response = reportService.getAllReports(status, districtId, landType, pageable);
        return ResponseEntity.ok(response);
    }

    // Authenticated staff - full detail including CNN fields
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<ReportResponseDTO> getReportById(@PathVariable Long id) {
        ReportResponseDTO response = reportService.getReportById(id);
        return ResponseEntity.ok(response);
    }

    // Authenticated staff - validates transition, sets resolvedBy/resolvedAt if RESOLVED
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<ReportResponseDTO> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {

        ReportResponseDTO response = reportService.updateReportStatus(id, dto, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    // Anonymous citizen - identity via X-Device-UUID header
    @GetMapping("/my")
    public ResponseEntity<PaginatedDTO> getMyReports(
            @RequestHeader("X-Device-UUID") String deviceUUID,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        validateDeviceUUID(deviceUUID);
        PaginatedDTO response = reportService.getMyReports(deviceUUID, pageable);
        return ResponseEntity.ok(response);
    }

    // Anonymous citizen - 404 (not 403) if report belongs to a different device
    @GetMapping("/my/{id}")
    public ResponseEntity<ReportResponseDTO> getMyReportById(
            @RequestHeader("X-Device-UUID") String deviceUUID,
            @PathVariable Long id) {

        validateDeviceUUID(deviceUUID);
        ReportResponseDTO response = reportService.getMyReportById(deviceUUID, id);
        return ResponseEntity.ok(response);
    }

    // District and PHI identity are always derived from the authenticated JWT — never from the request.
    @GetMapping("/phi/district")
    @PreAuthorize("hasRole('PHI')")
    public ResponseEntity<PaginatedDTO> getDistrictReports(
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        PaginatedDTO response = reportService.getDistrictReports(currentUser.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phi/district/resolved")
    @PreAuthorize("hasRole('PHI')")
    public ResponseEntity<PaginatedDTO> getDistrictResolvedReports(
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        PaginatedDTO response = reportService.getDistrictResolvedReports(currentUser.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phi/my-resolved")
    @PreAuthorize("hasRole('PHI')")
    public ResponseEntity<PaginatedDTO> getMyResolvedReports(
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        PaginatedDTO response = reportService.getMyResolvedReports(currentUser.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phi/district/resolved/{reportId}")
    @PreAuthorize("hasRole('PHI')")
    public ResponseEntity<ResolutionResponseDTO> getDistrictResolutionByReportId(
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDetails currentUser) {

        ResolutionResponseDTO response = resolutionService
                .getDistrictResolutionByReportId(reportId, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    private void validateDeviceUUID(String deviceUUID) {
        if (deviceUUID == null || deviceUUID.isBlank()) {
            throw new IllegalArgumentException("X-Device-UUID header must not be blank");
        }
        try {
            UUID.fromString(deviceUUID);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "X-Device-UUID header is not a valid UUID format: " + deviceUUID);
        }
    }
}