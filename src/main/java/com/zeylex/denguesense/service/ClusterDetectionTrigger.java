package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.ReportCluster;

public interface ClusterDetectionTrigger {
    void triggerForClassifiedReport(Long reportId);
    ReportCluster detectForDistrict(Long districtId);
}
