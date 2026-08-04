package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.event.ClusterDetectedEvent;
import com.zeylex.denguesense.exception.InvalidStateException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.ClusterMembership;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.repo.ClusterMembershipRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.service.ClusterDetectionService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClusterDetectionServiceImpl implements ClusterDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ClusterDetectionServiceImpl.class);
    private static final long DETECTION_LOCK_NAMESPACE = 8_847_201L;

    private final ReportClusterRepo reportClusterRepo;
    private final ClusterMembershipRepo clusterMembershipRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public ClusterDetectionServiceImpl(ReportClusterRepo reportClusterRepo,
                                       ClusterMembershipRepo clusterMembershipRepo,
                                       ApplicationEventPublisher eventPublisher,
                                       EntityManager entityManager) {
        this.reportClusterRepo = reportClusterRepo;
        this.clusterMembershipRepo = clusterMembershipRepo;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ReportCluster persistClusterDetection(Long districtId, List<Report> clusteredReports) {
        if (districtId == null) {
            throw new IllegalArgumentException("districtId must not be null");
        }
        if (clusteredReports == null || clusteredReports.isEmpty()) {
            throw new IllegalArgumentException("clusteredReports must not be null or empty");
        }

        acquireDistrictDetectionLock(districtId);

        ReportCluster cluster = reportClusterRepo
                .findActiveClusterForUpdate(districtId, ClusterStatus.ACTIVE)
                .orElse(null);

        boolean isNewCluster = false;
        if (cluster == null) {
            cluster = new ReportCluster();
            cluster.setDistrictId(districtId);
            cluster.setStatus(ClusterStatus.ACTIVE);
            cluster.setDetectedAt(LocalDateTime.now());
            cluster.setReportCount(0);
            cluster = reportClusterRepo.saveAndFlush(cluster);
            isNewCluster = true;
            log.info("Created ACTIVE cluster id={} for districtId={}", cluster.getId(), districtId);
        } else {
            cluster.getMemberships().size();
            log.debug("Reusing ACTIVE cluster id={} for districtId={}", cluster.getId(), districtId);
        }

        int added = 0;
        Set<Long> seenReportIds = new HashSet<>();
        for (Report report : clusteredReports) {
            if (report == null || report.getId() == null) {
                throw new IllegalArgumentException(
                        "Each clustered report must be persisted and have a non-null id");
            }
            if (!seenReportIds.add(report.getId())) {
                continue;
            }
            if (clusterMembershipRepo.existsByCluster_IdAndReport_Id(cluster.getId(), report.getId())) {
                continue;
            }
            ClusterMembership membership = new ClusterMembership();
            membership.setReport(report);
            cluster.addMembership(membership);
            added++;
        }

        cluster.setReportCount(cluster.getMemberships().size());

        boolean shouldAlert = cluster.getAlertedAt() == null && added > 0;
        if (shouldAlert) {
            cluster.setAlertedAt(LocalDateTime.now());
        }

        ReportCluster saved = reportClusterRepo.save(cluster);

        if (shouldAlert) {
            eventPublisher.publishEvent(new ClusterDetectedEvent(this, saved, isNewCluster));
            log.info("Published first ClusterDetectedEvent for cluster id={} (new={}, addedMemberships={}, reportCount={})",
                    saved.getId(), isNewCluster, added, saved.getReportCount());
        } else {
            log.debug(
                    "Skipping re-alert for cluster id={} (alertedAt={}, addedMemberships={})",
                    saved.getId(), saved.getAlertedAt(), added);
        }

        return saved;
    }

    @Override
    @Transactional
    public ReportCluster resolveCluster(Long clusterId) {
        if (clusterId == null) {
            throw new IllegalArgumentException("clusterId must not be null");
        }
        ReportCluster cluster = reportClusterRepo.findById(clusterId)
                .orElseThrow(() -> new NotFoundException("Cluster not found with id: " + clusterId));

        if (cluster.getStatus() == ClusterStatus.RESOLVED) {
            return cluster;
        }
        if (cluster.getStatus() != ClusterStatus.ACTIVE) {
            throw new InvalidStateException(
                    "Cluster id=" + clusterId + " cannot be resolved from status " + cluster.getStatus()
                            + ". Only ACTIVE clusters can be resolved.");
        }

        cluster.setStatus(ClusterStatus.RESOLVED);
        ReportCluster saved = reportClusterRepo.save(cluster);
        log.info("Cluster id={} marked RESOLVED", clusterId);
        return saved;
    }

    private void acquireDistrictDetectionLock(Long districtId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", detectionLockKey(districtId))
                .getResultList();
    }

    private static long detectionLockKey(Long districtId) {
        return DETECTION_LOCK_NAMESPACE * 1_000_003L + districtId;
    }
}
