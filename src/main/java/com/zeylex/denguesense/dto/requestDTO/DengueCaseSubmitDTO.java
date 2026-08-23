package com.zeylex.denguesense.dto.requestDTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DengueCaseSubmitDTO {

    private Long districtId;

    private String district;

    @NotNull(message = "weekStartDate is required")
    private LocalDate weekStartDate;

    private LocalDate weekEndDate;

    @NotNull(message = "weekCases is required")
    @Min(value = 0, message = "weekCases cannot be negative")
    private Integer weekCases;

    @Min(value = 0, message = "cumulativeCases cannot be negative")
    private Integer cumulativeCases;

    @AssertTrue(message = "district or districtId is required")
    public boolean hasDistrict() {
        return districtId != null || (district != null && !district.isBlank());
    }

    @AssertTrue(message = "weekEndDate must be on or after weekStartDate")
    public boolean hasValidWeekRange() {
        if (weekStartDate == null || weekEndDate == null) {
            return true;
        }
        return !weekEndDate.isBefore(weekStartDate);
    }
}
