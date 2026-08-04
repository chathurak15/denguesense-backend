package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;

import java.util.List;

public interface ClusterDetectionService {

    ReportCluster persistClusterDetection(Long districtId, List<Report> clusteredReports);

    ReportCluster resolveCluster(Long clusterId);
}
