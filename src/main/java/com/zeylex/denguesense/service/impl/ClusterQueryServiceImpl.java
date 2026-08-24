package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.ReportResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.ClusterMembership;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.ClusterMembershipRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.ClusterQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ClusterQueryServiceImpl implements ClusterQueryService {

    private static final Set<ClusterStatus> LIVE_STATUSES =
            EnumSet.of(ClusterStatus.ACTIVE, ClusterStatus.ALERTED);

    private final ReportClusterRepo reportClusterRepo;
    private final ClusterMembershipRepo clusterMembershipRepo;
    private final UserRepo userRepo;
    private final DistrictRepo districtRepo;

    public ClusterQueryServiceImpl(ReportClusterRepo reportClusterRepo,
                                   ClusterMembershipRepo clusterMembershipRepo,
                                   UserRepo userRepo,
                                   DistrictRepo districtRepo) {
        this.reportClusterRepo = reportClusterRepo;
        this.clusterMembershipRepo = clusterMembershipRepo;
        this.userRepo = userRepo;
        this.districtRepo = districtRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClusterResponseDTO> listLive(String userEmail, Long districtId) {
        User user = requireUser(userEmail);
        Long scopedDistrictId = resolveListDistrict(user, districtId);
        List<ReportCluster> clusters = scopedDistrictId == null
                ? reportClusterRepo.findByStatusInOrderByDetectedAtDesc(LIVE_STATUSES)
                : reportClusterRepo.findByDistrictIdAndStatusInOrderByIdAsc(
                        scopedDistrictId, LIVE_STATUSES);
        return clusters.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClusterResponseDTO getById(String userEmail, Long clusterId) {
        User user = requireUser(userEmail);
        ReportCluster cluster = reportClusterRepo.findById(clusterId)
                .orElseThrow(() -> new NotFoundException("Cluster not found with id: " + clusterId));
        assertCanView(user, cluster);
        return toDto(cluster);
    }

    private User requireUser(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + email);
        }
        return user;
    }

    private Long resolveListDistrict(User user, Long requestedDistrictId) {
        if (user.getRole() == RoleType.PHI) {
            if (user.getDistrict() == null) {
                throw new NotFoundException(
                        "PHI user '" + user.getEmail() + "' has no district assigned.");
            }
            return user.getDistrict().getId();
        }
        return requestedDistrictId;
    }

    private void assertCanView(User user, ReportCluster cluster) {
        if (user.getRole() != RoleType.PHI) {
            return;
        }
        if (user.getDistrict() == null
                || !user.getDistrict().getId().equals(cluster.getDistrictId())) {
            throw new NotFoundException("Cluster not found with id: " + cluster.getId());
        }
    }

    private ClusterResponseDTO toDto(ReportCluster cluster) {
        List<Report> reports = clusterMembershipRepo.findWithReportsByCluster_Id(cluster.getId())
                .stream()
                .map(ClusterMembership::getReport)
                .filter(r -> r != null)
                .toList();

        String districtName = districtRepo.findById(cluster.getDistrictId())
                .map(District::getName)
                .orElseGet(() -> reports.stream()
                        .map(Report::getDistrict)
                        .filter(d -> d != null)
                        .map(District::getName)
                        .findFirst()
                        .orElse("Unknown district"));

        double latSum = 0;
        double lngSum = 0;
        int geoCount = 0;
        int highRisk = 0;
        int privateLand = 0;
        int publicLand = 0;
        int unknownLand = 0;
        List<ReportResponseDTO> reportDtos = new ArrayList<>();
        for (Report report : reports) {
            reportDtos.add(toReportDto(report));
            if (report.getLatitude() != null && report.getLongitude() != null) {
                latSum += report.getLatitude();
                lngSum += report.getLongitude();
                geoCount++;
            }
            if (report.getCnnClassification() != null
                    && report.getCnnClassification().getRiskLabel() == RiskLabel.HIGH_RISK) {
                highRisk++;
            }
            LandType land = report.getLandType();
            if (land == LandType.PRIVATE) privateLand++;
            else if (land == LandType.PUBLIC) publicLand++;
            else unknownLand++;
        }

        int total = cluster.getReportCount() == null ? reports.size() : cluster.getReportCount();
        String risk = clusterRisk(highRisk, total);
        ClusterResponseDTO dto = new ClusterResponseDTO();
        dto.setId(cluster.getId());
        dto.setDistrictId(cluster.getDistrictId());
        dto.setDistrictName(districtName);
        dto.setStatus(cluster.getStatus());
        dto.setReportCount(total);
        dto.setLatitude(geoCount > 0 ? latSum / geoCount : null);
        dto.setLongitude(geoCount > 0 ? lngSum / geoCount : null);
        dto.setRisk(risk);
        dto.setInsight(insightFor(risk, privateLand, publicLand, unknownLand));
        dto.setDetectedAt(cluster.getDetectedAt());
        dto.setAlertedAt(cluster.getAlertedAt());
        dto.setReports(reportDtos);
        return dto;
    }

    private static String clusterRisk(int high, int total) {
        if (total <= 0) return "Low";
        double ratio = (double) high / total;
        if (ratio >= 0.4 || high >= 3 || total >= 5) return "High";
        if (high > 0) return "Medium";
        return "Low";
    }

    private static String insightFor(String risk, int privateLand, int publicLand, int unknownLand) {
        String landPhrase;
        if (privateLand >= publicLand && privateLand >= unknownLand && privateLand > 0) {
            landPhrase = "private premises (containers, tanks, and backyard drains)";
        } else if (publicLand >= unknownLand && publicLand > 0) {
            landPhrase = "public land (roadside drains and discarded containers)";
        } else {
            landPhrase = "mixed private and public sites";
        }
        if ("High".equals(risk)) {
            return "AI classification flags a high-risk cluster on " + landPhrase
                    + ". Prioritise PHI dispatch after recent rainfall.";
        }
        if ("Medium".equals(risk)) {
            return "AI model attributes this cluster mostly to " + landPhrase
                    + ". Monitor and verify remaining reports.";
        }
        return "Low-risk cluster on " + landPhrase
                + ". Continue routine inspection unless new high-risk reports arrive.";
    }

    private ReportResponseDTO toReportDto(Report report) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setLatitude(report.getLatitude());
        dto.setLongitude(report.getLongitude());
        dto.setImageUrl(report.getImageUrl());
        dto.setLandType(report.getLandType());
        dto.setReportStatus(report.getReportStatus());
        dto.setSubmittedAt(report.getSubmittedAt());
        dto.setDistrictName(report.getDistrict() != null ? report.getDistrict().getName() : null);
        if (report.getCnnClassification() != null) {
            dto.setCnnRiskLabel(report.getCnnClassification().getRiskLabel());
            dto.setCnnConfidenceScore(report.getCnnClassification().getConfidenceScore());
        }
        dto.setDispatchedAt(report.getDispatchedAt());
        if (report.getDispatchedBy() != null) {
            dto.setDispatchedByEmail(report.getDispatchedBy().getEmail());
        }
        dto.setResolvedAt(report.getResolvedAt());
        if (report.getResolvedBy() != null) {
            dto.setResolvedByEmail(report.getResolvedBy().getEmail());
        }
        return dto;
    }
}
