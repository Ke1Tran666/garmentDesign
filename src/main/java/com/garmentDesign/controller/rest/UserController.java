package com.garmentDesign.controller.rest;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.garmentDesign.dto.user.UpdateProfileRequest;
import com.garmentDesign.entity.User;
import com.garmentDesign.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService service;

	public UserController(UserService service) {
		this.service = service;
	}

	/*
	 * API quản trị
	 */

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public List<User> getAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public User getById(@PathVariable String id) {
		return service.findById(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public User create(@RequestBody User data) {
		return service.save(data);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		service.delete(id);

		return ResponseEntity.noContent().build();
	}

	/*
	 * API người dùng hiện tại
	 */

	@GetMapping("/me")
	public ResponseEntity<?> getMyProfile(Authentication authentication) {
		return ResponseEntity.ok(service.getProfile(authentication.getName()));
	}

	@PutMapping("/me")
	public User updateMyProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
		return service.updateProfile(authentication.getName(), request);
	}

	@PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
		return ResponseEntity.ok(service.uploadAvatar(authentication.getName(), file));
	}

	@DeleteMapping("/me/avatar")
	public ResponseEntity<?> deleteAvatar(Authentication authentication) {
		return ResponseEntity.ok(service.deleteAvatar(authentication.getName()));
	}

	@PutMapping("/me/change-password")
	public ResponseEntity<?> changePassword(Authentication authentication, @RequestBody Map<String, String> body,
			HttpServletRequest request) {

		HttpSession currentSession = request.getSession(false);

		if (currentSession == null) {
			throw new RuntimeException("Không thể xác định phiên đăng nhập hiện tại");
		}

		return ResponseEntity.ok(service.changePassword(authentication.getName(), body.get("oldPassword"),
				body.get("newPassword"), currentSession.getId()));
	}

	@GetMapping("/me/export-data")
	public ResponseEntity<?> exportUserData(Authentication authentication) {
		return ResponseEntity.ok(service.exportUserData(authentication.getName()));
	}

	@DeleteMapping("/me/delete-account")
	public ResponseEntity<?> deleteAccount(Authentication authentication) {
		return ResponseEntity.ok(service.deleteAccount(authentication.getName()));
	}

	@DeleteMapping("/me/phone/{providerId}")
	public ResponseEntity<?> deletePhone(Authentication authentication, @PathVariable Long providerId) {

		return ResponseEntity.ok(service.deletePhone(authentication.getName(), providerId));
	}
}