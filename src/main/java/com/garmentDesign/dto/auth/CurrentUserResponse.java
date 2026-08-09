package com.garmentDesign.dto.auth;

import java.time.LocalDate;

import com.garmentDesign.entity.User;

public record CurrentUserResponse(String idUser, String userCode, String fullName, String avatar, String gender,
		LocalDate birthday, String status, String role) {
	public static CurrentUserResponse from(User user) {
		String roleName = user.getRole() == null ? null : user.getRole().getNameRole();

		return new CurrentUserResponse(user.getIdUser(), user.getUserCode(), user.getFullName(), user.getAvatar(),
				user.getGender(), user.getBirthday(), user.getStatus(), roleName);
	}
}