package com.zeylex.denguesense.dto.requestDTO;

import com.zeylex.denguesense.model.enums.ResolutionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResolutionRequestDTO {
    @NotNull(message = "Resolution action is required")
    private ResolutionAction action;

    @NotBlank(message = "Resolution notes must not be blank")
    @Size(min = 1, max = 1000, message = "Notes must be between 1 and 1000 characters")
    private String notes;
}
