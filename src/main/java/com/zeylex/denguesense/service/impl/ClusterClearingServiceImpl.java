package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.model.ClusterMembership;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.repo.ClusterMembershipRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.service.ClusterClearingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClusterClearingServiceImpl implements ClusterClearingService {

    private static final Logger log = LoggerFactory.getLogger(ClusterClearingServiceImpl.class);

    private final ClusterMembershipRepo clusterMembershipRepo;
    private final ReportClusterRepo reportClusterRepo;

    public ClusterClearingServiceImpl(ClusterMembershipRepo clusterMembershipRepo,
                                      ReportClusterRepo reportClusterRepo) {
        this.clusterMembershipRepo = clusterMembershipRepo;
        this.reportClusterRepo = reportClusterRepo;
    }

    @Override
    @Transactional
    public void checkAndClearAffectedClusters(Report resolvedReport) {
        if (resolvedReport == null || resolvedReport.getId() == null) {
            log.warn("Skipping cluster clearing: resolved report id is null");
            return;
        }

        List<ClusterMembership> memberships = clusterMembershipRepo.findByReport_Id(resolvedReport.getId());
        if (memberships.isEmpty()) {
            log.info("Report id={} is not a member of any cluster; nothing to clear", resolvedReport.getId());
            return;
        }

        for (ClusterMembership membership : memberships) {
            Long clusterId = membership.getCluster().getId();
            evaluateCluster(clusterId, resolvedReport.getId());
        }
    }

    private void evaluateCluster(Long clusterId, Long triggerReportId) {
        ReportCluster cluster = reportClusterRepo.findByIdForUpdate(clusterId).orElse(null);
        if (cluster == null) {
            log.warn("Cluster id={} disappeared while clearing after report id={}", clusterId, triggerReportId);
            return;
        }

        if (cluster.getStatus() == ClusterStatus.CLEARED || cluster.getStatus() == ClusterStatus.EXPIRED) {
            log.info("Cluster id={} already {} — skipping (trigger report id={})",
                    clusterId, cluster.getStatus(), triggerReportId);
            return;
        }

        List<ClusterMembership> members = clusterMembershipRepo.findWithReportsByCluster_Id(clusterId);
        int unresolved = 0;
        for (ClusterMembership member : members) {
            Report memberReport = member.getReport();
            if (memberReport == null || memberReport.getReportStatus() != ReportStatus.RESOLVED) {
                unresolved++;
            }
        }

        if (unresolved == 0 && !members.isEmpty()) {
            cluster.setStatus(ClusterStatus.CLEARED);
            reportClusterRepo.save(cluster);
            log.info("Cluster id={} CLEARED — all {} member reports RESOLVED (trigger report id={})",
                    clusterId, members.size(), triggerReportId);
        } else {
            log.info("Cluster id={} remains {} — {} of {} member reports still unresolved (trigger report id={})",
                    clusterId, cluster.getStatus(), unresolved, members.size(), triggerReportId);
        }
    }
}
