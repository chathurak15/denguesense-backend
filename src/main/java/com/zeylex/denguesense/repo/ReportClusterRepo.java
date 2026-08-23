package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportClusterRepo extends JpaRepository<ReportCluster, Long> {

    Page<ReportCluster> findByDistrictIdAndStatus(Long districtId, ClusterStatus status, Pageable pageable);

    List<ReportCluster> findByDistrictIdAndStatus(Long districtId, ClusterStatus status);

    List<ReportCluster> findByStatusInOrderByDetectedAtDesc(Collection<ClusterStatus> statuses);

    Optional<ReportCluster> findFirstByDistrictIdAndStatusOrderByDetectedAtDesc(
            Long districtId, ClusterStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM ReportCluster c
            WHERE c.districtId = :districtId
              AND c.status IN (:statuses)
            ORDER BY c.detectedAt DESC
            LIMIT 1
            """)
    Optional<ReportCluster> findLiveClusterForUpdate(
            @Param("districtId") Long districtId,
            @Param("statuses") Collection<ClusterStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ReportCluster c WHERE c.id = :id")
    Optional<ReportCluster> findByIdForUpdate(@Param("id") Long id);
}
