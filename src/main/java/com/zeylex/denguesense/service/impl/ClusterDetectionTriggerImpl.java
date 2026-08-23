package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.config.ClusterDetectionProperties;
import com.zeylex.denguesense.model.CNNClassification;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.service.ClusterDetectionService;
import com.zeylex.denguesense.service.ClusterDetectionTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClusterDetectionTriggerImpl implements ClusterDetectionTrigger {

    private static final Logger log = LoggerFactory.getLogger(ClusterDetectionTriggerImpl.class);

    private final ReportRepo reportRepo;
    private final ClusterDetectionService clusterDetectionService;
    private final ClusterDetectionProperties props;

    public ClusterDetectionTriggerImpl(ReportRepo reportRepo,
                                       ClusterDetectionService clusterDetectionService,
                                       ClusterDetectionProperties props) {
        this.reportRepo = reportRepo;
        this.clusterDetectionService = clusterDetectionService;
        this.props = props;
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerForClassifiedReport(Long reportId) {
        if (reportId == null) {
            log.warn("Cluster detection skipped: null reportId");
            return;
        }

        Report report = reportRepo.findById(reportId).orElse(null);
        if (report == null) {
            log.warn("Cluster detection skipped: report id={} not found", reportId);
            return;
        }
        if (report.getReportStatus() != ReportStatus.CLASSIFIED) {
            log.debug("Cluster detection skipped: report id={} is {} (not CLASSIFIED)",
                    reportId, report.getReportStatus());
            return;
        }
        if (report.getDistrict() == null || report.getDistrict().getId() == null) {
            log.warn("Cluster detection skipped: report id={} has no district", reportId);
            return;
        }
        if (!isHighRisk(report)) {
            log.debug("Cluster detection skipped: report id={} is not HIGH_RISK; no hotspot contribution", reportId);
            return;
        }

        Long districtId = report.getDistrict().getId();
        LocalDateTime since = LocalDateTime.now().minusHours(props.getWindowHours());

        List<Report> neighbors = reportRepo.findActiveHighRiskNeighbors(
                reportId, districtId, since, props.getRadiusMeters());

        log.info("Cluster detection running for report id={} district id={}: {} HIGH_RISK neighbours within {}m / {}h window",
                reportId, districtId, neighbors.size(), props.getRadiusMeters(), props.getWindowHours());

        if (neighbors.size() < props.getMinClusterSize()) {
            log.info("Cluster detection: report id={} district id={} — {} neighbours < threshold {}, no cluster formed",
                    reportId, districtId, neighbors.size(), props.getMinClusterSize());
            return;
        }

        ReportCluster cluster = clusterDetectionService.persistClusterDetection(districtId, neighbors);
        log.info("Cluster detection complete: report id={} district id={} → clusterId={} status={} memberCount={}",
                reportId, districtId, cluster.getId(), cluster.getStatus(), cluster.getReportCount());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReportCluster detectForDistrict(Long districtId) {
        if (districtId == null) {
            throw new IllegalArgumentException("districtId must not be null");
        }

        LocalDateTime since = LocalDateTime.now().minusHours(props.getWindowHours());
        List<Report> active = reportRepo.findActiveHighRiskByDistrict(districtId, since);

        log.info("Manual cluster detection for district id={}: {} open HIGH_RISK reports in {}h window",
                districtId, active.size(), props.getWindowHours());

        if (active.size() < props.getMinClusterSize()) {
            log.info("Manual cluster detection: district id={} — {} reports < threshold {}, no cluster formed",
                    districtId, active.size(), props.getMinClusterSize());
            return null;
        }

        ReportCluster cluster = clusterDetectionService.persistClusterDetection(districtId, active);
        log.info("Manual cluster detection complete: district id={} → clusterId={} status={} memberCount={}",
                districtId, cluster.getId(), cluster.getStatus(), cluster.getReportCount());
        return cluster;
    }

    private static boolean isHighRisk(Report report) {
        CNNClassification cnn = report.getCnnClassification();
        return cnn != null && cnn.getRiskLabel() == RiskLabel.HIGH_RISK;
    }
}
