package com.garmentDesign.service;

import java.util.Map;

import com.garmentDesign.dto.auth.AuthenticatedUser;

public interface AuthService {

	AuthenticatedUser login(String email, String password);

	Map<String, Object> sendOtp(String phone);

	AuthenticatedUser verifyPhoneOtp(String phone, String otp);

	Map<String, Object> sendMyEmailOtp(String idUser);

	Map<String, Object> verifyMyEmailOtp(String idUser, String otp);

	Map<String, Object> forgotPassword(String email);

	Map<String, Object> verifyForgotOtp(String email, String otp);

	Map<String, Object> resetPassword(String email, String newPassword, String resetToken);

	AuthenticatedUser googleLogin(String credential);

	Map<String, Object> register(String email, String password, String fullName, String gender, String birthday);

	Map<String, Object> removeMyEmailVerification(String idUser);
}