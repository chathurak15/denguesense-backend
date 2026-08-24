package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.config.ClusterDetectionProperties;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ClusterDetectionServiceImpl implements ClusterDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ClusterDetectionServiceImpl.class);
    private static final long DETECTION_LOCK_NAMESPACE = 8_847_201L;
    private static final Set<ClusterStatus> LIVE_STATUSES =
            Set.of(ClusterStatus.ACTIVE, ClusterStatus.ALERTED);

    private final ReportClusterRepo reportClusterRepo;
    private final ClusterMembershipRepo clusterMembershipRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final ClusterDetectionProperties props;

    public ClusterDetectionServiceImpl(ReportClusterRepo reportClusterRepo,
                                       ClusterMembershipRepo clusterMembershipRepo,
                                       ApplicationEventPublisher eventPublisher,
                                       EntityManager entityManager,
                                       ClusterDetectionProperties props) {
        this.reportClusterRepo = reportClusterRepo;
        this.clusterMembershipRepo = clusterMembershipRepo;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.props = props;
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

        List<Long> incomingIds = clusteredReports.stream()
                .filter(Objects::nonNull)
                .map(Report::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (incomingIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Each clustered report must be persisted and have a non-null id");
        }

        List<ReportCluster> connected = lockSpatiallyConnectedLiveClusters(districtId, incomingIds);

        boolean isNewCluster = false;
        ReportCluster cluster;
        if (connected.isEmpty()) {
            cluster = new ReportCluster();
            cluster.setDistrictId(districtId);
            cluster.setStatus(ClusterStatus.ACTIVE);
            cluster.setDetectedAt(LocalDateTime.now());
            cluster.setReportCount(0);
            cluster = reportClusterRepo.saveAndFlush(cluster);
            isNewCluster = true;
            log.info("Created ACTIVE cluster id={} for districtId={} (no spatially connected live cluster)",
                    cluster.getId(), districtId);
        } else {
            cluster = connected.get(0);
            initializeMemberships(cluster);
            if (connected.size() > 1) {
                mergeInto(cluster, connected.subList(1, connected.size()));
            }
            log.info("Reusing live cluster id={} for districtId={} (connectedLiveClusters={})",
                    cluster.getId(), districtId, connected.size());
        }

        int added = addReports(cluster, clusteredReports);
        cluster.setReportCount(cluster.getMemberships().size());

        boolean shouldAlert = cluster.getAlertedAt() == null && added > 0;
        if (shouldAlert) {
            cluster.setAlertedAt(LocalDateTime.now());
        }

        ReportCluster saved = reportClusterRepo.saveAndFlush(cluster);

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

        if (cluster.getStatus() == ClusterStatus.CLEARED) {
            return cluster;
        }
        if (cluster.getStatus() == ClusterStatus.EXPIRED) {
            throw new InvalidStateException(
                    "Cluster id=" + clusterId + " is EXPIRED and cannot be cleared.");
        }
        if (cluster.getStatus() != ClusterStatus.ACTIVE && cluster.getStatus() != ClusterStatus.ALERTED) {
            throw new InvalidStateException(
                    "Cluster id=" + clusterId + " cannot be cleared from status " + cluster.getStatus()
                            + ". Only ACTIVE or ALERTED clusters can be cleared.");
        }

        cluster.setStatus(ClusterStatus.CLEARED);
        ReportCluster saved = reportClusterRepo.save(cluster);
        log.info("Cluster id={} marked CLEARED", clusterId);
        return saved;
    }

    private List<ReportCluster> lockSpatiallyConnectedLiveClusters(Long districtId, List<Long> incomingIds) {
        List<Long> connectedIds = toClusterIds(reportClusterRepo.findSpatiallyConnectedLiveClusterIds(
                districtId, incomingIds, props.getRadiusMeters()));
        if (connectedIds.isEmpty()) {
            return List.of();
        }

        List<Long> lockOrder = connectedIds.stream().distinct().sorted().toList();

        List<ReportCluster> live = new ArrayList<>();
        for (Long clusterId : lockOrder) {
            ReportCluster locked = reportClusterRepo.findByIdForUpdate(clusterId).orElse(null);
            if (locked == null) {
                continue;
            }
            if (!LIVE_STATUSES.contains(locked.getStatus())) {
                continue;
            }
            live.add(locked);
        }
        live.sort(Comparator
                .comparing(ReportCluster::getDetectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ReportCluster::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return live;
    }

    private void mergeInto(ReportCluster survivor, List<ReportCluster> absorbed) {
        for (ReportCluster other : absorbed) {
            initializeMemberships(other);
            List<Report> moving = other.getMemberships().stream()
                    .map(ClusterMembership::getReport)
                    .filter(report -> report != null && report.getId() != null)
                    .toList();
            other.getMemberships().clear();
            other.setReportCount(0);
            other.setStatus(ClusterStatus.EXPIRED);
            reportClusterRepo.saveAndFlush(other);
            int moved = addReports(survivor, moving);
            log.info("Merged cluster id={} into cluster id={} (movedMemberships={})",
                    other.getId(), survivor.getId(), moved);
        }
    }

    private int addReports(ReportCluster cluster, List<Report> clusteredReports) {
        initializeMemberships(cluster);
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
        return added;
    }

    private static void initializeMemberships(ReportCluster cluster) {
        if (cluster.getMemberships() == null) {
            cluster.setMemberships(new ArrayList<>());
        } else {
            cluster.getMemberships().size();
        }
    }

    private void acquireDistrictDetectionLock(Long districtId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", detectionLockKey(districtId))
                .getResultList();
    }

    private static List<Long> toClusterIds(List<?> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        return rawIds.stream()
                .filter(Objects::nonNull)
                .map(id -> id instanceof Number number ? number.longValue() : Long.parseLong(id.toString()))
                .toList();
    }

    private static long detectionLockKey(Long districtId) {
        return DETECTION_LOCK_NAMESPACE * 1_000_003L + districtId;
    }
}
