package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.ReportCluster;

import java.util.List;

public interface ClusterDetectionTrigger {
    void triggerForClassifiedReport(Long reportId);
    List<ReportCluster> detectForDistrict(Long districtId);
}
