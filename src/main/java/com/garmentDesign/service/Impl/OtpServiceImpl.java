package com.garmentDesign.service.Impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.garmentDesign.service.MailService;
import com.garmentDesign.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OtpServiceImpl.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final int OTP_EXPIRY_MINUTES = 5;
	private static final int VERIFIED_EXPIRY_MINUTES = 10;
	private static final int RESEND_SECONDS = 60;
	private static final int MAX_FAILED_ATTEMPTS = 5;

	private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

	private final Map<String, LocalDateTime> verifiedStorage = new ConcurrentHashMap<>();

	private final MailService mailService;
	private final PasswordEncoder passwordEncoder;
	private final boolean phoneConsoleEnabled;

	public OtpServiceImpl(MailService mailService, PasswordEncoder passwordEncoder,
			@Value("${app.otp.phone-console-enabled:false}") boolean phoneConsoleEnabled) {

		this.mailService = mailService;
		this.passwordEncoder = passwordEncoder;
		this.phoneConsoleEnabled = phoneConsoleEnabled;
	}

	@Override
	public void sendOtp(String target, String type) {
		String normalizedTarget = normalizeTarget(target, type);

		String key = buildKey(normalizedTarget, type);

		LocalDateTime now = LocalDateTime.now();

		OtpData existingOtp = otpStorage.get(key);

		if (existingOtp != null && existingOtp.getResendAvailableAt().isAfter(now)) {

			throw new RuntimeException("Vui lòng chờ 60 giây trước khi gửi lại OTP");
		}

		String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

		String encodedOtp = passwordEncoder.encode(otp);

		/*
		 * Chỉ lưu OTP sau khi kênh gửi xử lý thành công.
		 */
		if (isEmailOtp(type)) {
			mailService.sendOtpEmail(normalizedTarget, otp, type);
		} else if ("phone".equals(type)) {
			sendPhoneOtpForLocalDevelopment(normalizedTarget, otp);
		} else {
			throw new RuntimeException("Loại OTP không được hỗ trợ");
		}

		otpStorage.put(key,
				new OtpData(encodedOtp, now.plusMinutes(OTP_EXPIRY_MINUTES), now.plusSeconds(RESEND_SECONDS)));
	}

	@Override
	public boolean verifyOtp(String target, String type, String otp) {

		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("OTP không được để trống");
		}

		String normalizedTarget = normalizeTarget(target, type);

		String key = buildKey(normalizedTarget, type);

		OtpData otpData = otpStorage.get(key);

		if (otpData == null) {
			throw new RuntimeException("OTP không tồn tại hoặc chưa được gửi");
		}

		if (otpData.getExpiresAt().isBefore(LocalDateTime.now())) {

			otpStorage.remove(key, otpData);

			throw new RuntimeException("OTP đã hết hạn");
		}

		boolean correct = passwordEncoder.matches(otp.trim(), otpData.getEncodedOtp());

		if (!correct) {
			boolean attemptsExhausted = otpData.recordFailedAttempt();

			if (attemptsExhausted) {
				otpStorage.remove(key, otpData);

				throw new RuntimeException("Bạn đã nhập sai OTP quá 5 lần. " + "Vui lòng yêu cầu mã mới");
			}

			throw new RuntimeException("OTP không chính xác");
		}

		return true;
	}

	@Override
	public void markVerified(String target, String type) {

		String normalizedTarget = normalizeTarget(target, type);

		String key = buildKey(normalizedTarget, type);

		verifiedStorage.put(key, LocalDateTime.now().plusMinutes(VERIFIED_EXPIRY_MINUTES));
	}

	@Override
	public boolean isVerified(String target, String type) {

		String normalizedTarget = normalizeTarget(target, type);

		String key = buildKey(normalizedTarget, type);

		LocalDateTime expiresAt = verifiedStorage.get(key);

		if (expiresAt == null) {
			return false;
		}

		if (expiresAt.isBefore(LocalDateTime.now())) {
			verifiedStorage.remove(key, expiresAt);
			return false;
		}

		return true;
	}

	@Override
	public void clearOtp(String target, String type) {

		String normalizedTarget = normalizeTarget(target, type);

		otpStorage.remove(buildKey(normalizedTarget, type));
	}

	@Override
	public void clearVerified(String target, String type) {

		String normalizedTarget = normalizeTarget(target, type);

		verifiedStorage.remove(buildKey(normalizedTarget, type));
	}

	private String buildKey(String normalizedTarget, String type) {

		return type + ":" + normalizedTarget;
	}

	private String normalizeTarget(String target, String type) {

		if (target == null || target.isBlank()) {
			throw new RuntimeException("Thông tin nhận OTP không hợp lệ");
		}

		String normalized = target.trim();

		if (isEmailOtp(type)) {
			normalized = normalized.toLowerCase(Locale.ROOT);
		}

		return normalized;
	}

	private boolean isEmailOtp(String type) {
		return "verify-email".equals(type) || "reset-password".equals(type);
	}

	private void sendPhoneOtpForLocalDevelopment(String phone, String otp) {

		if (!phoneConsoleEnabled) {
			throw new RuntimeException("Kênh gửi OTP điện thoại chưa được cấu hình");
		}

		/*
		 * Chỉ dùng khi chạy local. Production phải đặt phone-console-enabled=false.
		 */
		LOGGER.warn("LOCAL PHONE OTP - phone: {}, otp: {}", phone, otp);
	}

	private static class OtpData {

		private final String encodedOtp;
		private final LocalDateTime expiresAt;
		private final LocalDateTime resendAvailableAt;

		private int failedAttempts;

		OtpData(String encodedOtp, LocalDateTime expiresAt, LocalDateTime resendAvailableAt) {

			this.encodedOtp = encodedOtp;
			this.expiresAt = expiresAt;
			this.resendAvailableAt = resendAvailableAt;
		}

		String getEncodedOtp() {
			return encodedOtp;
		}

		LocalDateTime getExpiresAt() {
			return expiresAt;
		}

		LocalDateTime getResendAvailableAt() {
			return resendAvailableAt;
		}

		synchronized boolean recordFailedAttempt() {
			failedAttempts++;
			return failedAttempts >= MAX_FAILED_ATTEMPTS;
		}
	}
}