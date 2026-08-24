package com.zeylex.denguesense.dto.responseDTO;

import com.zeylex.denguesense.model.enums.ClusterStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterResponseDTO {
    private Long id;
    private Long districtId;
    private String districtName;
    private ClusterStatus status;
    private Integer reportCount;
    private Double latitude;
    private Double longitude;
    private String risk;
    private String insight;
    private LocalDateTime detectedAt;
    private LocalDateTime alertedAt;
    private List<ReportResponseDTO> reports = new ArrayList<>();
}
