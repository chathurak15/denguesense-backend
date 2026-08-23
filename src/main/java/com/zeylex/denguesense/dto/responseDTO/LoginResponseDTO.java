package com.zeylex.denguesense.dto.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private UserResponseDTO user;
    private String token;

    private boolean otpRequired = false;
    private String email;

    public LoginResponseDTO(UserResponseDTO user, String token) {
        this.user = user;
        this.token = token;
        this.otpRequired = false;
    }
}
