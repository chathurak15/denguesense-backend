package com.zeylex.denguesense.dto.responseDTO;

import java.time.LocalDateTime;

public record CitizenAlertDTO(
        String id,
        String severity,
        String title,
        String body,
        String districtName,
        LocalDateTime createdAt
) {
}
