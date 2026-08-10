package com.garmentDesign.config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Configuration
public class GoogleAuthConfig {

	@Bean
	public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${app.google.client-id}") String googleClientId)
			throws GeneralSecurityException, IOException {

		if (googleClientId == null || googleClientId.isBlank()) {
			throw new IllegalStateException("GOOGLE_CLIENT_ID chưa được cấu hình");
		}

		return new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance())
				/*
				 * Chỉ chấp nhận ID token được Google cấp cho ứng dụng Garment Design.
				 */
				.setAudience(List.of(googleClientId.trim())).build();
	}
}