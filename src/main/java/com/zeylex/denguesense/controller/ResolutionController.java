package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.requestDTO.ResolutionRequestDTO;
import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
import com.zeylex.denguesense.service.ResolutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/resolutions")
@CrossOrigin
public class ResolutionController {

    private final ResolutionService resolutionService;

    public ResolutionController(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN')")
    public ResponseEntity<ResolutionResponseDTO> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ResolutionRequestDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {

        ResolutionResponseDTO response = resolutionService.resolveReport(
                reportId, dto, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<ResolutionResponseDTO> getResolutionByReportId(
            @PathVariable Long reportId) {

        ResolutionResponseDTO response = resolutionService.getResolutionByReportId(reportId);
        return ResponseEntity.ok(response);
    }
}
