package com.garmentDesign.service.Impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.garmentDesign.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService {

    private final Map<String, OtpData> otpStorage = new HashMap<>();
    private final Map<String, LocalDateTime> verifiedStorage = new HashMap<>();

    private String buildKey(String target, String type) {
        return type + ":" + target;
    }

    @Override
    public String sendOtp(String target, String type) {
        String otp = String.format("%06d", new Random().nextInt(1000000));

        String key = buildKey(target, type);

        otpStorage.put(key, new OtpData(
                otp,
                LocalDateTime.now().plusMinutes(5)
        ));

        /*
         * Sau này xử lý ở đây:
         * type = phone -> gửi SMS
         * type = email -> gửi mail
         */
        System.out.println("OTP " + type + " của " + target + ": " + otp);

        return otp;
    }

    @Override
    public boolean verifyOtp(String target, String type, String otp) {
        String key = buildKey(target, type);

        OtpData otpData = otpStorage.get(key);

        if (otpData == null) {
            throw new RuntimeException("OTP không tồn tại hoặc chưa được gửi");
        }

        if (otpData.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpStorage.remove(key);
            throw new RuntimeException("OTP đã hết hạn");
        }

        if (!otpData.getOtp().equals(otp)) {
            throw new RuntimeException("OTP không chính xác");
        }

        return true;
    }

    @Override
    public void markVerified(String target, String type) {
        String key = buildKey(target, type);
        verifiedStorage.put(key, LocalDateTime.now().plusMinutes(10));
    }

    @Override
    public boolean isVerified(String target, String type) {
        String key = buildKey(target, type);

        LocalDateTime expiresAt = verifiedStorage.get(key);

        if (expiresAt == null) {
            return false;
        }

        if (expiresAt.isBefore(LocalDateTime.now())) {
            verifiedStorage.remove(key);
            return false;
        }

        return true;
    }

    @Override
    public void clearOtp(String target, String type) {
        otpStorage.remove(buildKey(target, type));
    }

    @Override
    public void clearVerified(String target, String type) {
        verifiedStorage.remove(buildKey(target, type));
    }

    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiresAt;

        public OtpData(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }
}