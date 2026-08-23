package com.zeylex.denguesense.dto.responseDTO;

import com.zeylex.denguesense.model.enums.ResolutionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ResolutionResponseDTO {
    private Long id;
    private Long reportId;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private ResolutionAction action;
    private String notes;
}
