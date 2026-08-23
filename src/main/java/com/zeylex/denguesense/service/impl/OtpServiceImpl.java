package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.service.OtpService;
import org.springframework.stereotype.Service;

@Service
public class OtpServiceImpl implements OtpService {
    @Override
    public void generateAndSendOtp(String email, String purpose) {

    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        return false;
    }

    @Override
    public void clearOtp(String email) {

    }
}
