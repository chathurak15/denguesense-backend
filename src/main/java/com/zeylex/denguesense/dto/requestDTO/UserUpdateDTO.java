package com.zeylex.denguesense.dto.requestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    @NotBlank(message = "Email is required to identify the user")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "First name cannot be empty")
    private String fname;
    @NotBlank(message = "Last name cannot be empty")
    private String lname;
    @Pattern(regexp = "^\\+?[0-9]{10,12}$", message = "Invalid phone number format")
    private String phoneNumber;
    private String image;

    @NotBlank(message = "District name cannot be empty")
    private int districtId;
}
