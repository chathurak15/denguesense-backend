package com.zeylex.denguesense.dto.responseDTO;

public record CitizenDistrictDTO(
        Long id,
        String name,
        String province,
        Integer rdhsId,
        Double latitude,
        Double longitude
) {
}
