package com.zeylex.denguesense.service;

import com.zeylex.denguesense.config.ClusterDetectionProperties;
import com.zeylex.denguesense.model.CNNClassification;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.service.impl.ClusterDetectionTriggerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterDetectionTrigger — inline spatial scoping + threshold gating")
class ClusterDetectionTriggerImplTest {

    @Mock private ReportRepo reportRepo;
    @Mock private ClusterDetectionService clusterDetectionService;

    private ClusterDetectionTriggerImpl trigger;

    private static final Long DISTRICT_ID = 1L;   // Colombo
    private static final Long REPORT_ID = 13L;

    @BeforeEach
    void setUp() {
        ClusterDetectionProperties props = new ClusterDetectionProperties();
        props.setRadiusMeters(500.0);
        props.setMinClusterSize(5);
        props.setWindowHours(24);
        trigger = new ClusterDetectionTriggerImpl(reportRepo, clusterDetectionService, props);
    }

    private Report highRiskClassified(long id) {
        Report r = new Report();
        r.setId(id);
        r.setReportStatus(ReportStatus.CLASSIFIED);
        District d = new District();
        d.setId(DISTRICT_ID);
        d.setName("Colombo");
        r.setDistrict(d);
        CNNClassification cnn = new CNNClassification();
        cnn.setRiskLabel(RiskLabel.HIGH_RISK);
        r.setCnnClassification(cnn);
        return r;
    }

    private List<Report> neighbours(int n) {
        List<Report> list = new ArrayList<>();
        IntStream.rangeClosed(1, n).forEach(i -> list.add(highRiskClassified(100 + i)));
        return list;
    }

    @Test
    @DisplayName("5 HIGH_RISK reports within 500m → persistClusterDetection called once with all 5")
    void fiveWithinRadius_formsOneCluster() {
        Report reference = highRiskClassified(REPORT_ID);
        when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(reference));
        List<Report> five = neighbours(5);
        when(reportRepo.findActiveHighRiskNeighbors(eq(REPORT_ID), eq(DISTRICT_ID), any(LocalDateTime.class), anyDouble()))
                .thenReturn(five);
        when(clusterDetectionService.persistClusterDetection(eq(DISTRICT_ID), any()))
                .thenReturn(cluster(1L, ClusterStatus.ACTIVE, 5));

        trigger.triggerForClassifiedReport(REPORT_ID);

        ArgumentCaptor<List<Report>> captor = ArgumentCaptor.forClass(List.class);
        verify(clusterDetectionService).persistClusterDetection(eq(DISTRICT_ID), captor.capture());
        assertThat(captor.getValue()).hasSize(5);
    }

    @Test
    @DisplayName("6th nearby report → still delegates to persistClusterDetection (reuse handled by service)")
    void sixthReport_delegatesToService() {
        Report reference = highRiskClassified(REPORT_ID);
        when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(reference));
        when(reportRepo.findActiveHighRiskNeighbors(eq(REPORT_ID), eq(DISTRICT_ID), any(LocalDateTime.class), anyDouble()))
                .thenReturn(neighbours(6));
        when(clusterDetectionService.persistClusterDetection(eq(DISTRICT_ID), any()))
                .thenReturn(cluster(1L, ClusterStatus.ALERTED, 6));

        trigger.triggerForClassifiedReport(REPORT_ID);

        ArgumentCaptor<List<Report>> captor = ArgumentCaptor.forClass(List.class);
        verify(clusterDetectionService).persistClusterDetection(eq(DISTRICT_ID), captor.capture());
        assertThat(captor.getValue()).hasSize(6);
    }

    @Test
    @DisplayName("Fewer than min-cluster-size neighbours → no cluster is created")
    void belowThreshold_noCluster() {
        Report reference = highRiskClassified(REPORT_ID);
        when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(reference));
        when(reportRepo.findActiveHighRiskNeighbors(eq(REPORT_ID), eq(DISTRICT_ID), any(LocalDateTime.class), anyDouble()))
                .thenReturn(neighbours(4));

        trigger.triggerForClassifiedReport(REPORT_ID);

        verify(clusterDetectionService, never()).persistClusterDetection(anyLong(), any());
    }

    @Test
    @DisplayName("Non-HIGH_RISK report → skipped entirely (no spatial query, no cluster)")
    void notHighRisk_skipped() {
        Report reference = highRiskClassified(REPORT_ID);
        reference.getCnnClassification().setRiskLabel(RiskLabel.LOW_RISK);
        when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(reference));

        trigger.triggerForClassifiedReport(REPORT_ID);

        verify(reportRepo, never())
                .findActiveHighRiskNeighbors(anyLong(), anyLong(), any(LocalDateTime.class), anyDouble());
        verify(clusterDetectionService, never()).persistClusterDetection(anyLong(), any());
    }

    @Test
    @DisplayName("Report not in CLASSIFIED state → skipped")
    void notClassified_skipped() {
        Report reference = highRiskClassified(REPORT_ID);
        reference.setReportStatus(ReportStatus.PENDING);
        when(reportRepo.findById(REPORT_ID)).thenReturn(Optional.of(reference));

        trigger.triggerForClassifiedReport(REPORT_ID);

        verify(clusterDetectionService, never()).persistClusterDetection(anyLong(), any());
    }

    @Test
    @DisplayName("detectForDistrict → delegates when district has enough open HIGH_RISK reports")
    void detectForDistrict_delegates() {
        when(reportRepo.findActiveHighRiskByDistrict(eq(DISTRICT_ID), any(LocalDateTime.class)))
                .thenReturn(neighbours(5));
        when(clusterDetectionService.persistClusterDetection(eq(DISTRICT_ID), any()))
                .thenReturn(cluster(9L, ClusterStatus.ACTIVE, 5));

        ReportCluster result = trigger.detectForDistrict(DISTRICT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(9L);
        verify(clusterDetectionService).persistClusterDetection(eq(DISTRICT_ID), any());
    }

    private static ReportCluster cluster(Long id, ClusterStatus status, int count) {
        ReportCluster c = new ReportCluster();
        c.setId(id);
        c.setDistrictId(DISTRICT_ID);
        c.setStatus(status);
        c.setReportCount(count);
        return c;
    }
}
