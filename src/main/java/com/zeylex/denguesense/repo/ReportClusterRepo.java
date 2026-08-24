package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportClusterRepo extends JpaRepository<ReportCluster, Long> {

    Page<ReportCluster> findByDistrictIdAndStatus(Long districtId, ClusterStatus status, Pageable pageable);

    List<ReportCluster> findByDistrictIdAndStatus(Long districtId, ClusterStatus status);

    List<ReportCluster> findByDistrictIdAndStatusInOrderByIdAsc(
            Long districtId, Collection<ClusterStatus> statuses);

    List<ReportCluster> findByStatusInOrderByDetectedAtDesc(Collection<ClusterStatus> statuses);

    Optional<ReportCluster> findFirstByDistrictIdAndStatusOrderByDetectedAtDesc(
            Long districtId, ClusterStatus status);

    /**
     * Live clusters in the district that spatially connect to any of {@code reportIds}:
     * at least one existing member is within {@code radiusMeters} of an incoming report
     * ({@code ST_DWithin} on geography, so the radius is metres).
     */
    @Query(value = """
            SELECT DISTINCT c.id
            FROM report_cluster c
            JOIN cluster_membership m ON m.cluster_id = c.id
            JOIN reports existing ON existing.id = m.report_id
            JOIN reports incoming ON incoming.id IN (:reportIds)
            WHERE c.district_id = :districtId
              AND c.status IN ('ACTIVE', 'ALERTED')
              AND existing.location IS NOT NULL
              AND incoming.location IS NOT NULL
              AND ST_DWithin(existing.location, incoming.location, :radiusMeters)
            """, nativeQuery = true)
    List<Long> findSpatiallyConnectedLiveClusterIds(
            @Param("districtId") Long districtId,
            @Param("reportIds") Collection<Long> reportIds,
            @Param("radiusMeters") double radiusMeters);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ReportCluster c WHERE c.id = :id")
    Optional<ReportCluster> findByIdForUpdate(@Param("id") Long id);

    /**
     * Scalar status change only — avoids merging the cluster graph (memberships have
     * {@code cascade = ALL, orphanRemoval = true}), which can fail after a successful Telegram send.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReportCluster c
               SET c.status = :toStatus
             WHERE c.id = :clusterId
               AND c.status = :fromStatus
            """)
    int updateStatusIfCurrent(@Param("clusterId") Long clusterId,
                              @Param("fromStatus") ClusterStatus fromStatus,
                              @Param("toStatus") ClusterStatus toStatus);
}
