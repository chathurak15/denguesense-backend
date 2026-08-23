package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.service.ClusterDetectionTrigger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/v1/admin/clusters")
public class ClusterController {

    private final ClusterDetectionTrigger clusterDetectionTrigger;

    public ClusterController(ClusterDetectionTrigger clusterDetectionTrigger) {
        this.clusterDetectionTrigger = clusterDetectionTrigger;
    }

    @PostMapping("/detect")
    @PreAuthorize("hasAnyRole('ADMIN','PHI')")
    public ResponseEntity<Map<String, Object>> detect(@RequestParam Long districtId) {
        ReportCluster cluster = clusterDetectionTrigger.detectForDistrict(districtId);
        if (cluster == null) {
            return ResponseEntity.ok(Map.of(
                    "districtId", districtId,
                    "clusterFormed", false,
                    "message", "No cluster met the detection threshold for this district."));
        }
        return ResponseEntity.ok(Map.of(
                "districtId", districtId,
                "clusterFormed", true,
                "clusterId", cluster.getId(),
                "status", cluster.getStatus(),
                "reportCount", cluster.getReportCount()));
    }
}
