package com.zeylex.denguesense.dto.responseDTO;

import com.zeylex.denguesense.model.enums.LandType;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportResponseDTO {
    private Long id;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private LandType landType;
    private ReportStatus reportStatus;
    private String districtName;
    private RiskLabel cnnRiskLabel;
    private Double cnnConfidenceScore;
    private LocalDateTime submittedAt;
    private String dispatchedByEmail;
    private LocalDateTime dispatchedAt;
    private String resolvedByEmail;
    private LocalDateTime resolvedAt;
    private String resolvedByDisplayName;
    private ResolutionResponseDTO resolution;
}