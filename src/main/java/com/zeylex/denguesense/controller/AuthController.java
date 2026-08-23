package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.requestDTO.LoginDTO;
import com.zeylex.denguesense.dto.requestDTO.RegisterDTO;
import com.zeylex.denguesense.dto.responseDTO.LoginResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.service.JwtService;
import com.zeylex.denguesense.service.OtpService;
import com.zeylex.denguesense.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("api/v1/auth")
@RestController
@CrossOrigin
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDTO registerDTO) {
        String result = userService.registerUser(registerDTO);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        LoginResponseDTO result = jwtService.createJwtToken(loginDTO);

        if (result.isOtpRequired()) {
            // 2FA path — return 202 Accepted so the frontend shows the OTP screen
            return ResponseEntity.accepted().body(
                    Map.of("otpRequired", true, "email", result.getEmail())
            );
        }

        return buildJwtCookieResponse(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<UserResponseDTO> buildJwtCookieResponse(LoginResponseDTO loginResponseDTO) {
        ResponseCookie cookie = ResponseCookie.from("JWT", loginResponseDTO.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(12 * 60 * 60)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponseDTO.getUser());
    }
}
