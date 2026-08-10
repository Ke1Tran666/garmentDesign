package com.garmentDesign.service;

public interface OtpService {

	void sendOtp(String target, String type);

	boolean verifyOtp(String target, String type, String otp);

	String createVerificationToken(String target, String type);

	boolean consumeVerificationToken(String target, String type, String token);

	void clearOtp(String target, String type);
}