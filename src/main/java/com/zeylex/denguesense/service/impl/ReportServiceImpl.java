package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.requestDTO.ReportStatusUpdateDTO;
import com.zeylex.denguesense.dto.requestDTO.ReportSubmitDTO;
import com.zeylex.denguesense.dto.ai.ClassifyResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.ResolutionResponseDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.CNNClassification;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.Resolution;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.AiClassificationService;
import com.zeylex.denguesense.service.CloudinaryService;
import com.zeylex.denguesense.service.DistrictService;
import com.zeylex.denguesense.service.ReportService;
import com.zeylex.denguesense.util.GeoUtils;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReportStatus.PENDING,    EnumSet.of(ReportStatus.CLASSIFIED, ReportStatus.DISMISSED, ReportStatus.REJECTED),
            ReportStatus.CLASSIFIED, EnumSet.of(ReportStatus.DISPATCHED, ReportStatus.DISMISSED, ReportStatus.REJECTED),
            ReportStatus.DISPATCHED, EnumSet.of(ReportStatus.DISMISSED, ReportStatus.REJECTED),
            ReportStatus.RESOLVED,   EnumSet.noneOf(ReportStatus.class),
            ReportStatus.DISMISSED,  EnumSet.noneOf(ReportStatus.class),
            ReportStatus.REJECTED,   EnumSet.noneOf(ReportStatus.class)
    );

    private final ReportRepo reportRepo;
    private final UserRepo userRepo;
    private final DistrictService districtService;
    private final CloudinaryService cloudinaryService;
    private final AiClassificationService aiClassificationService;

    public ReportServiceImpl(ReportRepo reportRepo,
                             UserRepo userRepo,
                             DistrictService districtService,
                             CloudinaryService cloudinaryService,
                             AiClassificationService aiClassificationService) {
        this.reportRepo                = reportRepo;
        this.userRepo                  = userRepo;
        this.districtService           = districtService;
        this.cloudinaryService         = cloudinaryService;
        this.aiClassificationService   = aiClassificationService;
    }

    @Override
    @Transactional
    public ReportResponseDTO saveReport(String deviceUUID, ReportSubmitDTO dto, MultipartFile image) {
        String imageUrl = cloudinaryService.uploadReportImage(image, deviceUUID);
        District district = districtService.findByCoordinates(dto.getLatitude(), dto.getLongitude());

        Report report = new Report();
        report.setDeviceUUID(deviceUUID);
        report.setLatitude(dto.getLatitude());
        report.setLongitude(dto.getLongitude());
        report.setLocation(GeoUtils.toPoint(dto.getLatitude(), dto.getLongitude()));
        report.setLandType(dto.getLandType());
        report.setImageUrl(imageUrl);
        report.setCnnClassification(null);
        report.setDistrict(district);
        report.setReportStatus(ReportStatus.PENDING);

        Report saved = reportRepo.save(report);

        // CNN Classification
        try {
            ClassifyResponseDTO classification = aiClassificationService.classify(imageUrl);

            if (classification.riskLabel() == null) {
                throw new IllegalArgumentException("AI service returned null risk label for imageUrl: " + imageUrl);
            }
            RiskLabel riskLabel = RiskLabel.valueOf(classification.riskLabel());

            CNNClassification cnn = new CNNClassification();
            cnn.setReport(saved);
            cnn.setRiskLabel(riskLabel);
            cnn.setConfidenceScore(classification.confidenceScore() != null ? classification.confidenceScore() : 0.0);
            cnn.setModelVersion(classification.modelVersion());
            saved.setCnnClassification(cnn);
            saved.setReportStatus(ReportStatus.CLASSIFIED);

            if (riskLabel == RiskLabel.INVALID) {
                saved.setReportStatus(ReportStatus.REJECTED);
            }

            saved = reportRepo.save(saved);

        } catch (AiServiceException | IllegalArgumentException ex) {
            log.warn("CNN classification failed for report id={}, imageUrl={}. "
                    + "Report saved with status=PENDING and no classification. Reason: {}",
                    saved.getId(), imageUrl, ex.getMessage());
        }
        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO getAllReports(ReportStatus status, Long districtId,
                                     LandType landType, Pageable pageable) {
        Specification<Report> spec = buildFilterSpec(status, districtId, landType);
        Page<Report> page = reportRepo.findAll(spec, pageable);
        List<ReportResponseDTO> items = page.getContent().stream()
                .map(this::toResponseDTO)
                .toList();
        return new PaginatedDTO(items, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponseDTO getReportById(Long id) {
        Report report = reportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + id));
        return toResponseDTO(report);
    }

    @Override
    @Transactional
    public ReportResponseDTO updateReportStatus(Long id, ReportStatusUpdateDTO dto,
                                                String currentUserEmail) {
        Report report = reportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + id));

        ReportStatus current = report.getReportStatus();
        ReportStatus target  = dto.getStatus();

        // Hard block — RESOLVED must go through the Resolution endpoint
        if (target == ReportStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Cannot set report status to RESOLVED via this endpoint. " +
                    "Use POST /api/v1/resolutions/" + id + " to resolve a report. " +
                    "This ensures a Resolution record is created with proper notes and action.");
        }

        Set<ReportStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(ReportStatus.class));
        if (!allowed.contains(target)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + current + " → " + target +
                    ". Allowed targets from " + current + ": " + allowed);
        }

        report.setReportStatus(target);

        if (target == ReportStatus.DISPATCHED) {
            if (report.getDispatchedAt() == null) {
                report.setDispatchedAt(java.time.LocalDateTime.now());
            }
            if (report.getDispatchedBy() == null) {
                User dispatcher = userRepo.findByEmail(currentUserEmail);
                if (dispatcher != null) {
                    report.setDispatchedBy(dispatcher);
                } else {
                    log.warn("Dispatcher email={} not found in users table — dispatchedBy will be null for report id={}",
                            currentUserEmail, id);
                }
            }
        }

        Report updated = reportRepo.save(report);
        return toResponseDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO getMyReports(String deviceUUID, Pageable pageable) {
        Page<Report> page = reportRepo.findByDeviceUUID(deviceUUID, pageable);
        List<ReportResponseDTO> items = page.getContent().stream()
                .map(this::toCitizenDTO)
                .toList();
        return new PaginatedDTO(items, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponseDTO getMyReportById(String deviceUUID, Long id) {
        Report report = reportRepo.findByIdAndDeviceUUID(id, deviceUUID)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + id));
        return toCitizenDTO(report);
    }

    // ── PHI district-scoped views ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO getDistrictReports(String phiEmail, Pageable pageable) {
        Long districtId = resolvePhiDistrictId(phiEmail);
        Page<Report> page = reportRepo.findByDistrict_Id(districtId, pageable);
        List<ReportResponseDTO> items = page.getContent().stream()
                .map(this::toResponseDTO)
                .toList();
        return new PaginatedDTO(items, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO getDistrictResolvedReports(String phiEmail, Pageable pageable) {
        Long districtId = resolvePhiDistrictId(phiEmail);
        Page<Report> page = reportRepo.findByDistrict_IdAndReportStatus(
                districtId, ReportStatus.RESOLVED, pageable);
        List<ReportResponseDTO> items = page.getContent().stream()
                .map(this::toResponseDTO)
                .toList();
        return new PaginatedDTO(items, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO getMyResolvedReports(String phiEmail, Pageable pageable) {
        User phi = userRepo.findByEmail(phiEmail);
        if (phi == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + phiEmail);
        }
        Page<Report> page = reportRepo.findByResolvedBy_IdAndReportStatus(
                phi.getId(), ReportStatus.RESOLVED, pageable);
        List<ReportResponseDTO> items = page.getContent().stream()
                .map(this::toResponseDTO)
                .toList();
        return new PaginatedDTO(items, page.getTotalPages(), page.getTotalElements());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Loads the authenticated PHI's district ID from the database.
     * Throws NotFoundException if the user doesn't exist or has no assigned district.
     */
    private Long resolvePhiDistrictId(String phiEmail) {
        User phi = userRepo.findByEmail(phiEmail);
        if (phi == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + phiEmail);
        }
        if (phi.getDistrict() == null) {
            throw new NotFoundException(
                    "PHI user '" + phiEmail + "' has no district assigned. " +
                    "Contact your administrator to assign a district.");
        }
        return phi.getDistrict().getId();
    }

    private Specification<Report> buildFilterSpec(ReportStatus status, Long districtId, LandType landType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("reportStatus"), status));
            }
            if (districtId != null) {
                predicates.add(cb.equal(root.get("district").get("id"), districtId));
            }
            if (landType != null) {
                predicates.add(cb.equal(root.get("landType"), landType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Full staff/internal response — includes email tracking fields.
     * Used for: GET /all, GET /{id}, PATCH /{id}/status, and PHI district views.
     */
    private ReportResponseDTO toResponseDTO(Report report) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setLatitude(report.getLatitude());
        dto.setLongitude(report.getLongitude());
        dto.setLandType(report.getLandType());
        dto.setReportStatus(report.getReportStatus());
        dto.setSubmittedAt(report.getSubmittedAt());
        dto.setDistrictName(report.getDistrict() != null ? report.getDistrict().getName() : null);

        if (report.getCnnClassification() != null) {
            dto.setCnnRiskLabel(report.getCnnClassification().getRiskLabel());
            dto.setCnnConfidenceScore(report.getCnnClassification().getConfidenceScore());
        }

        // Dispatch tracking (staff only)
        dto.setDispatchedAt(report.getDispatchedAt());
        if (report.getDispatchedBy() != null) {
            dto.setDispatchedByEmail(report.getDispatchedBy().getEmail());
        }

        // Resolution tracking (staff only)
        dto.setResolvedAt(report.getResolvedAt());
        if (report.getResolvedBy() != null) {
            dto.setResolvedByEmail(report.getResolvedBy().getEmail());
        }

        // Embed resolution details
        if (report.getResolution() != null) {
            dto.setResolution(buildResolutionDTO(report));
        }

        return dto;
    }

    /**
     * Citizen-safe response — strips all email/phone/internal ID fields.
     * Exposes only: resolvedByDisplayName (safe display name) and safe resolution sub-object.
     * Used for: GET /my, GET /my/{id}.
     */
    private ReportResponseDTO toCitizenDTO(Report report) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setLatitude(report.getLatitude());
        dto.setLongitude(report.getLongitude());
        dto.setLandType(report.getLandType());
        dto.setReportStatus(report.getReportStatus());
        dto.setSubmittedAt(report.getSubmittedAt());
        dto.setDistrictName(report.getDistrict() != null ? report.getDistrict().getName() : null);

        if (report.getCnnClassification() != null) {
            dto.setCnnRiskLabel(report.getCnnClassification().getRiskLabel());
            dto.setCnnConfidenceScore(report.getCnnClassification().getConfidenceScore());
        }

        // Resolution info — only shown when the report is actually resolved
        if (report.getReportStatus() == ReportStatus.RESOLVED && report.getResolution() != null) {
            Resolution resolution = report.getResolution();
            User resolver = resolution.getResolvedBy();
            if (resolver != null) {
                String displayName = ((resolver.getFname() != null ? resolver.getFname() : "") +
                                     (resolver.getLname() != null ? " " + resolver.getLname() : "")).trim();
                dto.setResolvedByDisplayName(displayName);
            }
            dto.setResolvedAt(resolution.getResolvedAt());

            // Safe embedded resolution: no email, no phone, no internal user ID
            ResolutionResponseDTO resDto = ResolutionResponseDTO.builder()
                    .id(resolution.getId())
                    .reportId(report.getId())
                    .resolvedByName(dto.getResolvedByDisplayName())
                    .resolvedAt(resolution.getResolvedAt())
                    .action(resolution.getAction())
                    .notes(resolution.getNotes())
                    .build();
            dto.setResolution(resDto);
        }

        return dto;
    }

    /**
     * Builds the embedded ResolutionResponseDTO for staff views (includes resolver name).
     */
    private ResolutionResponseDTO buildResolutionDTO(Report report) {
        Resolution resolution = report.getResolution();
        User resolver = resolution.getResolvedBy();
        String resolverName = "";
        if (resolver != null) {
            resolverName = ((resolver.getFname() != null ? resolver.getFname() : "") +
                           (resolver.getLname() != null ? " " + resolver.getLname() : "")).trim();
        }
        return ResolutionResponseDTO.builder()
                .id(resolution.getId())
                .reportId(report.getId())
                .resolvedByName(resolverName)
                .resolvedAt(resolution.getResolvedAt())
                .action(resolution.getAction())
                .notes(resolution.getNotes())
                .build();
    }
}