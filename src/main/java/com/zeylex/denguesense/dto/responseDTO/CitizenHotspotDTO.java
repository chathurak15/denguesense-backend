package com.zeylex.denguesense.dto.responseDTO;

public record CitizenHotspotDTO(
        String id,
        String districtName,
        double latitude,
        double longitude,
        String risk,
        int reportCount
) {
}
