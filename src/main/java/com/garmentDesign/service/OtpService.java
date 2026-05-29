package com.garmentDesign.service;

public interface OtpService {

    String sendOtp(String target, String type);

    boolean verifyOtp(String target, String type, String otp);

    void markVerified(String target, String type);

    boolean isVerified(String target, String type);

    void clearOtp(String target, String type);

    void clearVerified(String target, String type);
}