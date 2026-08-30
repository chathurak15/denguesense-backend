package com.zeylex.denguesense.service;

import com.zeylex.denguesense.config.ClusterDetectionProperties;
import com.zeylex.denguesense.event.ClusterDetectedEvent;
import com.zeylex.denguesense.model.ClusterMembership;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.repo.ClusterMembershipRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.service.impl.ClusterDetectionServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterDetectionService — spatial reuse vs new cluster")
class ClusterDetectionServiceImplTest {

    private static final Long DISTRICT_ID = 1L;

    @Mock private ReportClusterRepo reportClusterRepo;
    @Mock private ClusterMembershipRepo clusterMembershipRepo;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private Query lockQuery;

    private ClusterDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        ClusterDetectionProperties props = new ClusterDetectionProperties();
        props.setRadiusMeters(500.0);
        service = new ClusterDetectionServiceImpl(
                reportClusterRepo, clusterMembershipRepo, eventPublisher, entityManager, props);

        when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
        when(lockQuery.setParameter(anyString(), any())).thenReturn(lockQuery);
        when(lockQuery.getResultList()).thenReturn(List.of());
        when(reportClusterRepo.saveAndFlush(any(ReportCluster.class))).thenAnswer(inv -> {
            ReportCluster cluster = inv.getArgument(0);
            if (cluster.getId() == null) {
                cluster.setId(new AtomicLong(10).incrementAndGet());
            }
            return cluster;
        });
    }

    @Test
    @DisplayName("No spatially connected live cluster → create a new ACTIVE cluster and alert")
    void noOverlap_createsNewCluster() {
        when(reportClusterRepo.findSpatiallyConnectedLiveClusterIds(anyLong(), any(), anyDouble()))
                .thenReturn(List.of());

        ReportCluster saved = service.persistClusterDetection(DISTRICT_ID, reports(21L, 22L, 23L, 24L, 25L));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ClusterStatus.ACTIVE);
        assertThat(saved.getDistrictId()).isEqualTo(DISTRICT_ID);
        assertThat(saved.getReportCount()).isEqualTo(5);
        assertThat(saved.getAlertedAt()).isNotNull();
        assertThat(saved.getMemberships()).hasSize(5);

        ArgumentCaptor<ClusterDetectedEvent> captor = ArgumentCaptor.forClass(ClusterDetectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isNewCluster()).isTrue();
        verify(reportClusterRepo, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("Hotspot within 500m of an already-alerted cluster → add members, do not re-alert")
    void overlapAlertedCluster_reusesWithoutRealert() {
        ReportCluster existing = liveCluster(1L, ClusterStatus.ALERTED, LocalDateTime.now().minusDays(3));
        existing.setAlertedAt(existing.getDetectedAt());
        stubConnected(existing);

        ReportCluster saved = service.persistClusterDetection(DISTRICT_ID, reports(21L, 22L, 23L, 24L, 25L));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getReportCount()).isEqualTo(5);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Hotspot within 500m of a live cluster that was never alerted → reuse and send first alert")
    void overlapUnalertedCluster_reusesAndAlerts() {
        ReportCluster existing = liveCluster(1L, ClusterStatus.ACTIVE, LocalDateTime.now().minusHours(2));
        stubConnected(existing);

        ReportCluster saved = service.persistClusterDetection(DISTRICT_ID, reports(21L, 22L, 23L, 24L, 25L));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getAlertedAt()).isNotNull();
        ArgumentCaptor<ClusterDetectedEvent> captor = ArgumentCaptor.forClass(ClusterDetectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isNewCluster()).isFalse();
    }

    @Test
    @DisplayName("Hotspot within 500m of two live clusters → merge into the oldest, expire the other")
    void overlapTwoClusters_mergesIntoOldest() {
        ReportCluster older = liveCluster(1L, ClusterStatus.ALERTED, LocalDateTime.now().minusDays(3));
        older.setAlertedAt(older.getDetectedAt());
        ReportCluster newer = liveCluster(2L, ClusterStatus.ACTIVE, LocalDateTime.now().minusHours(1));
        Report alreadyInNewer = report(50L);
        ClusterMembership membership = new ClusterMembership();
        membership.setReport(alreadyInNewer);
        newer.addMembership(membership);

        when(reportClusterRepo.findSpatiallyConnectedLiveClusterIds(anyLong(), any(), anyDouble()))
                .thenReturn(List.of(2L, 1L));
        when(reportClusterRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(older));
        when(reportClusterRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(newer));
        when(clusterMembershipRepo.existsByCluster_IdAndReport_Id(anyLong(), anyLong())).thenReturn(false);

        ReportCluster saved = service.persistClusterDetection(DISTRICT_ID, reports(21L, 22L, 23L, 24L, 25L));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(newer.getStatus()).isEqualTo(ClusterStatus.EXPIRED);
        assertThat(newer.getMemberships()).isEmpty();
        assertThat(saved.getMemberships()).extracting(m -> m.getReport().getId())
                .contains(50L, 21L, 22L, 23L, 24L, 25L);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void stubConnected(ReportCluster existing) {
        when(reportClusterRepo.findSpatiallyConnectedLiveClusterIds(anyLong(), any(), anyDouble()))
                .thenReturn(List.of(existing.getId()));
        when(reportClusterRepo.findByIdForUpdate(existing.getId())).thenReturn(Optional.of(existing));
        when(clusterMembershipRepo.existsByCluster_IdAndReport_Id(anyLong(), anyLong())).thenReturn(false);
    }

    private static ReportCluster liveCluster(Long id, ClusterStatus status, LocalDateTime detectedAt) {
        ReportCluster cluster = new ReportCluster();
        cluster.setId(id);
        cluster.setDistrictId(DISTRICT_ID);
        cluster.setStatus(status);
        cluster.setDetectedAt(detectedAt);
        cluster.setReportCount(0);
        return cluster;
    }

    private static List<Report> reports(long... ids) {
        return java.util.Arrays.stream(ids).mapToObj(ClusterDetectionServiceImplTest::report).toList();
    }

    private static Report report(long id) {
        Report report = new Report();
        report.setId(id);
        return report;
    }
}
