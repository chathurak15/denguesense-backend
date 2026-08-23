package com.zeylex.denguesense.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClassifyResponseDTO(
        @JsonProperty("riskLabel") String riskLabel,
        @JsonProperty("confidenceScore") Double confidenceScore,
        @JsonProperty("modelVersion") String modelVersion
) {
}
