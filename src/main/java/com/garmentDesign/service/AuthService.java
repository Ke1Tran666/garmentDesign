package com.garmentDesign.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String email, String password);

    Map<String, Object> loginPhone(String phone);

    Map<String, Object> sendOtp(String phone);

    Map<String, Object> verifyOtp(String phone, String otp);

    Map<String, Object> forgotPassword(String email);

    Map<String, Object> verifyForgotOtp(String email, String otp);

    Map<String, Object> resetPassword(String email, String newPassword);
    
    Map<String, Object> googleLogin(String accessToken);

    Map<String, Object> register(
            String email,
            String password,
            String fullName,
            String gender,
            String birthday
    );
}