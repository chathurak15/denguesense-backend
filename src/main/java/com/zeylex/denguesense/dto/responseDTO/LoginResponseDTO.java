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

    // OTP / 2FA fields — populated when credentials are valid but OTP is required
    private boolean otpRequired = false;
    private String email;

    // Convenience constructor for direct login (READER / post-OTP)
    public LoginResponseDTO(UserResponseDTO user, String token) {
        this.user = user;
        this.token = token;
        this.otpRequired = false;
    }
}
