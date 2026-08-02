package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.ClusterMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterMembershipRepo extends JpaRepository<ClusterMembership, Long> {

    List<ClusterMembership> findByCluster_Id(Long clusterId);

    boolean existsByCluster_IdAndReport_Id(Long clusterId, Long reportId);
}
