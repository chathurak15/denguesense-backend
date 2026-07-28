package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.requestDTO.ResolutionRequestDTO;
import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
import com.zeylex.denguesense.exception.DuplicationException;
import com.zeylex.denguesense.exception.InvalidStateException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.Resolution;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.repo.ResolutionRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.NotificationService;
import com.zeylex.denguesense.service.ResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class ResolutionServiceImpl implements ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionServiceImpl.class);

    private static final Set<ReportStatus> TERMINAL_STATUSES =
            EnumSet.of(ReportStatus.RESOLVED, ReportStatus.REJECTED, ReportStatus.DISMISSED);

    private final ReportRepo            reportRepo;
    private final ResolutionRepo resolutionRepository;
    private final UserRepo              userRepo;
    private final NotificationService   notificationService;

    public ResolutionServiceImpl(ReportRepo reportRepo,
                                 ResolutionRepo resolutionRepository,
                                 UserRepo userRepo,
                                 NotificationService notificationService) {
        this.reportRepo           = reportRepo;
        this.resolutionRepository = resolutionRepository;
        this.userRepo             = userRepo;
        this.notificationService  = notificationService;
    }


    @Override
    @Transactional
    public ResolutionResponseDTO resolveReport(Long reportId,
                                               ResolutionRequestDTO dto,
                                               String phiEmail) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + reportId));

        ReportStatus current = report.getReportStatus();
        if (TERMINAL_STATUSES.contains(current)) {
            throw new InvalidStateException(
                    "Report id=" + reportId + " is already in a terminal state: " + current +
                    ". Only DISPATCHED reports can be resolved.");
        }

        if (current != ReportStatus.DISPATCHED) {
            throw new InvalidStateException(
                    "Report id=" + reportId + " must be in DISPATCHED state before it can be resolved. " +
                    "Current state: " + current + ". Use PATCH /api/v1/reports/" + reportId +
                    "/status to dispatch first.");
        }

        if (resolutionRepository.existsByReport_Id(reportId)) {
            throw new DuplicationException(
                    "A resolution already exists for report id=" + reportId +
                    ". Each report may only be resolved once.");
        }

        User phi = userRepo.findByEmail(phiEmail);
        if (phi == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + phiEmail);
        }

        Resolution resolution = new Resolution();
        resolution.setReport(report);
        resolution.setResolvedBy(phi);
        resolution.setAction(dto.getAction());
        resolution.setNotes(dto.getNotes());
        Resolution saved = resolutionRepository.save(resolution);

        report.setReportStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(saved.getResolvedAt());
        report.setResolvedBy(phi);
        report.setResolution(saved);
        reportRepo.save(report);

        log.info("Report id={} resolved by user={} (action={}) at {}",
                reportId, phiEmail, dto.getAction(), saved.getResolvedAt());

        try {
            notificationService.notifyResolved(report);
        } catch (Exception ex) {
            log.warn("Notification failed for resolved report id={}: {}", reportId, ex.getMessage());
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolutionResponseDTO getResolutionByReportId(Long reportId) {
        if (!reportRepo.existsById(reportId)) {
            throw new NotFoundException("Report not found with id: " + reportId);
        }
        Resolution resolution = resolutionRepository.findByReport_Id(reportId)
                .orElseThrow(() -> new NotFoundException(
                        "No resolution found for report id: " + reportId +
                        ". The report may not have been resolved yet."));
        return toResponseDTO(resolution);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolutionResponseDTO getDistrictResolutionByReportId(Long reportId, String phiEmail) {
        User phi = userRepo.findByEmail(phiEmail);
        if (phi == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + phiEmail);
        }
        if (phi.getDistrict() == null) {
            throw new NotFoundException(
                    "PHI user '" + phiEmail + "' has no district assigned. " +
                    "Contact your administrator to assign a district.");
        }
        Long districtId = phi.getDistrict().getId();

        Resolution resolution = resolutionRepository
                .findByReport_IdAndReport_District_Id(reportId, districtId)
                .orElseThrow(() -> new NotFoundException(
                        "No resolution found for report id: " + reportId +
                        " in district id: " + districtId +
                        ". The report may not belong to your district or may not have been resolved yet."));

        return toResponseDTO(resolution);
    }
    private ResolutionResponseDTO toResponseDTO(Resolution resolution) {
        User resolver = resolution.getResolvedBy();
        String name = (resolver.getFname() != null ? resolver.getFname() : "") +
                      (resolver.getLname() != null ? " " + resolver.getLname() : "");

        return ResolutionResponseDTO.builder()
                .id(resolution.getId())
                .reportId(resolution.getReport().getId())
                .resolvedByName(name.trim())
                .resolvedAt(resolution.getResolvedAt())
                .action(resolution.getAction())
                .notes(resolution.getNotes())
                .build();
    }
}
