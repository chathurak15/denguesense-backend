package com.zeylex.denguesense.service;

public interface OtpService {
    void generateAndSendOtp(String email, String purpose);
    boolean verifyOtp(String email, String otp);
    void clearOtp(String email);
}
