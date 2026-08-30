package com.zeylex.denguesense.dto.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramAlertStatusDTO {
    private boolean connected;
    private String connectUrl;
    private String botUsername;
}
