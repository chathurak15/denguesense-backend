package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.ClusterMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClusterMembershipRepo extends JpaRepository<ClusterMembership, Long> {

    List<ClusterMembership> findByCluster_Id(Long clusterId);

    List<ClusterMembership> findByReport_Id(Long reportId);

    boolean existsByCluster_IdAndReport_Id(Long clusterId, Long reportId);

    @Query("""
            SELECT DISTINCT m FROM ClusterMembership m
            JOIN FETCH m.report r
            LEFT JOIN FETCH r.cnnClassification
            LEFT JOIN FETCH r.district
            WHERE m.cluster.id = :clusterId
            """)
    List<ClusterMembership> findWithReportsByCluster_Id(@Param("clusterId") Long clusterId);
}
