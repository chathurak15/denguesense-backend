package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.service.ClusterQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/clusters")
public class ClusterQueryController {

    private final ClusterQueryService clusterQueryService;

    public ClusterQueryController(ClusterQueryService clusterQueryService) {
        this.clusterQueryService = clusterQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<List<ClusterResponseDTO>> listLive(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(required = false) Long districtId) {
        return ResponseEntity.ok(
                clusterQueryService.listLive(currentUser.getUsername(), districtId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHI','MOH','ADMIN','EPIDEMIOLOGIST')")
    public ResponseEntity<ClusterResponseDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(clusterQueryService.getById(currentUser.getUsername(), id));
    }
}
