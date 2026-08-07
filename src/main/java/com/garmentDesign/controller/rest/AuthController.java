package com.garmentDesign.controller.rest;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentDesign.dto.auth.AuthenticatedUser;
import com.garmentDesign.dto.auth.CurrentUserResponse;
import com.garmentDesign.entity.User;
import com.garmentDesign.service.AuthService;
import com.garmentDesign.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthService authService, UserService userService,
			SecurityContextRepository securityContextRepository) {
		this.authService = authService;
		this.userService = userService;
		this.securityContextRepository = securityContextRepository;
	}

	private void establishSession(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
		if (user == null) {
			throw new RuntimeException("Không thể tạo phiên đăng nhập");
		}

		if (user.idUser() == null || user.idUser().isBlank()) {
			throw new RuntimeException("Người dùng không hợp lệ");
		}

		if (user.role() == null || user.role().isBlank()) {
			throw new RuntimeException("Tài khoản chưa được phân quyền");
		}

		String normalizedRole = user.role().trim().toUpperCase();

		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalizedRole);

		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(user.idUser(), null,
				List.of(authority));

		// Đổi ID session cũ để chống session fixation.
		HttpSession existingSession = request.getSession(false);

		if (existingSession != null) {
			request.changeSessionId();
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();

		context.setAuthentication(authentication);

		SecurityContextHolder.setContext(context);

		securityContextRepository.saveContext(context, request, response);
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request,
			HttpServletResponse response) {
		AuthenticatedUser user = authService.login(body.get("email"), body.get("password"));

		establishSession(user, request, response);

		return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công"));
	}

	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.sendOtp(body.get("phone")));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyPhoneOtp(@RequestBody Map<String, String> body, HttpServletRequest request,
			HttpServletResponse response) {
		AuthenticatedUser user = authService.verifyPhoneOtp(body.get("phone"), body.get("otp"));

		establishSession(user, request, response);

		return ResponseEntity.ok(Map.of("message", "Xác thực số điện thoại thành công"));
	}

	@PostMapping("/send-email-otp")
	public ResponseEntity<?> sendEmailOtp(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.sendEmailOtp(body.get("email")));
	}

	@PostMapping("/verify-email-otp")
	public ResponseEntity<?> verifyEmailOtp(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.verifyEmailOtp(body.get("email"), body.get("otp")));
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.register(body.get("email"), body.get("password"), body.get("fullName"),
				body.get("gender"), body.get("birthday")));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.forgotPassword(body.get("email")));
	}

	@PostMapping("/verify-forgot-otp")
	public ResponseEntity<?> verifyForgotOtp(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.verifyForgotOtp(body.get("email"), body.get("otp")));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
		return ResponseEntity.ok(authService.resetPassword(body.get("email"), body.get("newPassword")));
	}

	@PostMapping("/google-login")
	public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body, HttpServletRequest request,
			HttpServletResponse response) {
		AuthenticatedUser user = authService.googleLogin(body.get("accessToken"));

		establishSession(user, request, response);

		return ResponseEntity.ok(Map.of("message", "Đăng nhập Google thành công"));
	}

	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken csrfToken) {
		return csrfToken;
	}

	@GetMapping("/me")
	public ResponseEntity<CurrentUserResponse> currentUser(Authentication authentication) {
		boolean anonymous = authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken;

		if (anonymous) {
			return ResponseEntity.noContent().build();
		}

		User user = userService.findById(authentication.getName());

		return ResponseEntity.ok(CurrentUserResponse.from(user));
	}
}