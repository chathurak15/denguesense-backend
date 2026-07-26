package com.zeylex.denguesense.dto.requestDTO;

import com.zeylex.denguesense.model.enums.ReportStatus;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    @AssertFalse(message = "Cannot set status to RESOLVED via this endpoint. Use POST /api/v1/resolutions/{reportId} instead.")
    public boolean isResolutionAttemptedViaStatusEndpoint() {
        return status == ReportStatus.RESOLVED;
    }
}