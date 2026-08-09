package com.garmentDesign.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final PasswordEncoder passwordEncoder;

	public PasswordService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void validateNewPassword(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new RuntimeException("Mật khẩu không được để trống");
		}

		if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new RuntimeException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
		}
	}

	public String encode(String rawPassword) {
		validateNewPassword(rawPassword);
		return passwordEncoder.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String storedPassword) {
		if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
			return false;
		}

		if (isBcrypt(storedPassword)) {
			return passwordEncoder.matches(rawPassword, storedPassword);
		}

		/*
		 * Tương thích tạm thời với mật khẩu plaintext cũ. Sau khi đăng nhập thành công,
		 * mật khẩu sẽ được đổi sang BCrypt.
		 */
		return MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8),
				storedPassword.getBytes(StandardCharsets.UTF_8));
	}

	public boolean needsUpgrade(String storedPassword) {
		if (!isBcrypt(storedPassword)) {
			return true;
		}

		return passwordEncoder.upgradeEncoding(storedPassword);
	}

	private boolean isBcrypt(String storedPassword) {
		return storedPassword != null && storedPassword.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
	}
}