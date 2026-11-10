package com.garmentDesign.dto.auth;

public record AuthenticatedUser(String idUser, String role, Long roleId) {
	public AuthenticatedUser(String idUser, String role) {
		this(idUser, role, null);
	}
}
