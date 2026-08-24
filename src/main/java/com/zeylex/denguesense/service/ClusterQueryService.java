package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;

import java.util.List;

public interface ClusterQueryService {
    List<ClusterResponseDTO> listLive(String userEmail, Long districtId);
    ClusterResponseDTO getById(String userEmail, Long clusterId);
}
