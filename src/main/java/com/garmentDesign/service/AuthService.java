package com.garmentDesign.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String email, String password);
    
    Map<String, Object> loginPhone(String phone);
    
    Map<String, Object> sendOtp(String phone);

    Map<String, Object> verifyOtp(String phone, String otp);
}