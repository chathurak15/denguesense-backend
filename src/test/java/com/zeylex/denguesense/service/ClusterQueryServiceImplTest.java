package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.ClusterMembership;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.ClusterMembershipRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.ClusterQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterQueryService — backend cluster rows for PHI dashboard")
class ClusterQueryServiceImplTest {

    @Mock private ReportClusterRepo reportClusterRepo;
    @Mock private ClusterMembershipRepo clusterMembershipRepo;
    @Mock private UserRepo userRepo;
    @Mock private DistrictRepo districtRepo;

    private ClusterQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClusterQueryServiceImpl(
                reportClusterRepo, clusterMembershipRepo, userRepo, districtRepo);
    }

    @Test
    @DisplayName("GET by id returns database cluster id and membership report count")
    void getById_usesPersistedCluster() {
        User phi = phiUser();
        ReportCluster cluster = cluster(2L, 5);
        when(userRepo.findByEmail("phi@health.gov.lk")).thenReturn(phi);
        when(reportClusterRepo.findById(2L)).thenReturn(Optional.of(cluster));
        when(districtRepo.findById(1L)).thenReturn(Optional.of(phi.getDistrict()));
        when(clusterMembershipRepo.findWithReportsByCluster_Id(2L))
                .thenReturn(List.of(
                        membership(cluster, report(21L, 6.82, 79.92)),
                        membership(cluster, report(22L, 6.83, 79.93)),
                        membership(cluster, report(23L, 6.84, 79.94)),
                        membership(cluster, report(24L, 6.85, 79.95)),
                        membership(cluster, report(25L, 6.86, 79.96))
                ));

        ClusterResponseDTO dto = service.getById("phi@health.gov.lk", 2L);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getReportCount()).isEqualTo(5);
        assertThat(dto.getReports()).hasSize(5);
        assertThat(dto.getDistrictName()).isEqualTo("Colombo");
        assertThat(dto.getLatitude()).isNotNull();
        assertThat(dto.getLongitude()).isNotNull();
    }

    @Test
    @DisplayName("PHI cannot load a cluster from another district")
    void getById_hidesOtherDistrict() {
        User phi = phiUser();
        ReportCluster other = cluster(9L, 3);
        other.setDistrictId(99L);
        when(userRepo.findByEmail("phi@health.gov.lk")).thenReturn(phi);
        when(reportClusterRepo.findById(9L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getById("phi@health.gov.lk", 9L))
                .isInstanceOf(NotFoundException.class);
    }

    private static User phiUser() {
        District district = new District();
        district.setId(1L);
        district.setName("Colombo");
        User user = new User();
        user.setId(10L);
        user.setEmail("phi@health.gov.lk");
        user.setRole(RoleType.PHI);
        user.setDistrict(district);
        return user;
    }

    private static ReportCluster cluster(Long id, int count) {
        ReportCluster cluster = new ReportCluster();
        cluster.setId(id);
        cluster.setDistrictId(1L);
        cluster.setReportCount(count);
        cluster.setStatus(ClusterStatus.ACTIVE);
        cluster.setDetectedAt(LocalDateTime.now());
        return cluster;
    }

    private static Report report(Long id, double lat, double lng) {
        District district = new District();
        district.setId(1L);
        district.setName("Colombo");
        Report report = new Report();
        report.setId(id);
        report.setLatitude(lat);
        report.setLongitude(lng);
        report.setLandType(LandType.PUBLIC);
        report.setReportStatus(ReportStatus.CLASSIFIED);
        report.setDistrict(district);
        return report;
    }

    private static ClusterMembership membership(ReportCluster cluster, Report report) {
        ClusterMembership membership = new ClusterMembership();
        membership.setCluster(cluster);
        membership.setReport(report);
        return membership;
    }
}
