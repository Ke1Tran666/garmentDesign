package com.garmentDesign.service;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

	private final SessionRegistry sessionRegistry;

	public UserSessionService(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	public void registerSession(String sessionId, String idUser) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new RuntimeException("Session ID không hợp lệ");
		}

		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Người dùng không hợp lệ");
		}

		/*
		 * Tránh đăng ký lặp cùng một session.
		 */
		SessionInformation existing = sessionRegistry.getSessionInformation(sessionId);

		if (existing != null) {
			sessionRegistry.removeSessionInformation(sessionId);
		}

		sessionRegistry.registerNewSession(sessionId, idUser);
	}

	public void removeSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return;
		}

		sessionRegistry.removeSessionInformation(sessionId);
	}

	public void expireAllSessions(String idUser) {
		if (idUser == null || idUser.isBlank()) {
			return;
		}

		sessionRegistry.getAllSessions(idUser, false).forEach(SessionInformation::expireNow);
	}
}