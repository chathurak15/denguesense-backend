package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.service.ClusterDetectionTrigger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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
        List<ReportCluster> clusters = clusterDetectionTrigger.detectForDistrict(districtId);
        if (clusters == null || clusters.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "districtId", districtId,
                    "clusterFormed", false,
                    "message", "No cluster met the detection threshold for this district."));
        }

        ReportCluster primary = clusters.get(0);
        List<Map<String, Object>> summaries = clusters.stream()
                .map(cluster -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("clusterId", cluster.getId());
                    summary.put("status", cluster.getStatus());
                    summary.put("reportCount", cluster.getReportCount());
                    return summary;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("districtId", districtId);
        body.put("clusterFormed", true);
        body.put("clusterId", primary.getId());
        body.put("status", primary.getStatus());
        body.put("reportCount", primary.getReportCount());
        body.put("clusters", summaries);
        return ResponseEntity.ok(body);
    }
}
