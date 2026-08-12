package com.garmentDesign.service.Impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentDesign.dto.auth.AuthenticatedUser;
import com.garmentDesign.entity.Role;
import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.AuthService;
import com.garmentDesign.service.OtpService;
import com.garmentDesign.service.PasswordService;
import com.garmentDesign.service.UserStatusService;

import java.time.format.DateTimeParseException;
import java.util.List;

import java.util.Locale;

import com.garmentDesign.service.UserSessionService;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserAuthProviderRepository authProviderRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final OtpService otpService;
	private final PasswordService passwordService;
	private final UserStatusService userStatusService;
	private final GoogleIdTokenVerifier googleIdTokenVerifier;
	private final UserSessionService userSessionService;

	private static final String VERIFY_EMAIL_OTP = "verify-email";
	private static final String RESET_PASSWORD_OTP = "reset-password";

	public AuthServiceImpl(UserAuthProviderRepository authProviderRepository, UserRepository userRepository,
			RoleRepository roleRepository, OtpService otpService, PasswordService passwordService,
			UserStatusService userStatusService, GoogleIdTokenVerifier googleIdTokenVerifier,
			UserSessionService userSessionService) {
		this.authProviderRepository = authProviderRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.otpService = otpService;
		this.passwordService = passwordService;
		this.userStatusService = userStatusService;
		this.googleIdTokenVerifier = googleIdTokenVerifier;
		this.userSessionService = userSessionService;
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new RuntimeException("Email không được để trống");
		}

		return email.trim().toLowerCase(Locale.ROOT);
	}

	private void validateUserStatus(User user) {
		if (user == null) {
			throw new RuntimeException("Không tìm thấy tài khoản");
		}

		if ("inactive".equalsIgnoreCase(user.getStatus())) {

			throw new RuntimeException(
					"Tài khoản của bạn đang tạm ngưng hoạt động. " + "Vui lòng liên hệ hotline để được hỗ trợ.");
		}

		if ("banned".equalsIgnoreCase(user.getStatus())) {

			throw new RuntimeException("Tài khoản của bạn đã bị khóa. " + "Vui lòng liên hệ hotline để được hỗ trợ.");
		}

		if ("delete".equalsIgnoreCase(user.getStatus())) {

			throw new RuntimeException(
					"Tài khoản của bạn đã bị xóa. " + "Nếu muốn khôi phục, vui lòng liên hệ hotline.");
		}

		/*
		 * Không chặn pending.
		 */
	}

	private AuthenticatedUser createLoginResult(User user) {
		if (user.getRole() == null) {
			throw new RuntimeException("Tài khoản chưa được phân quyền");
		}

		return new AuthenticatedUser(user.getIdUser(), user.getRole().getNameRole());
	}

	private User createPendingGoogleUser(String fullName) {
		String idUser = generateRandom5Number();
		String prefixName = generateNameCode(fullName);
		String userCode = prefixName + "U00" + idUser;

		Role userRole = roleRepository.findById(3L).orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

		User user = new User();
		user.setIdUser(idUser);
		user.setUserCode(userCode);
		user.setFullName(fullName);
		user.setGender("Unknown");

		/*
		 * Google đã xác thực email, nhưng tài khoản vẫn chưa đủ hồ sơ và địa chỉ.
		 */
		user.setStatus("pending");
		user.setRole(userRole);

		return userRepository.save(user);
	}

	private String removeVietnameseAccent(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

		return normalized.replaceAll("\\p{M}", "").replace("Đ", "D").replace("đ", "d");
	}

	private String generateNameCode(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			return "USE";
		}

		String cleanName = removeVietnameseAccent(fullName).trim().replaceAll("\\s+", " ");

		String[] words = cleanName.split(" ");

		String lastName = words[words.length - 1].replaceAll("[^a-zA-Z]", "").toUpperCase();

		if (lastName.length() >= 3) {
			return lastName.substring(0, 3);
		}

		return String.format("%-3s", lastName).replace(' ', 'O');
	}

	private String generateRandom5Number() {
		String id;

		do {
			id = String.format("%05d", new Random().nextInt(100000));
		} while (userRepository.existsById(id));

		return id;
	}

	private User createPendingPhoneUser() {
		String idUser = generateRandom5Number();
		String userCode = "USEU00" + idUser;

		Role userRole = roleRepository.findById(3L).orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

		User user = new User();
		user.setIdUser(idUser);
		user.setUserCode(userCode);
		user.setGender("Unknown");
		user.setStatus("pending");
		user.setRole(userRole);

		return userRepository.save(user);
	}

	private User getUserForLinking(String idUser) {
		if (idUser == null || idUser.trim().isEmpty()) {
			return createPendingPhoneUser();
		}

		User user = userRepository.findById(idUser)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng để liên kết"));

		validateUserStatus(user);

		return user;
	}

	private UserAuthProvider getActiveLocalProviderForReset(String normalizedEmail) {
		UserAuthProvider auth = authProviderRepository
				.findByEmailAndProviderAndDeletedAtIsNull(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Email không tồn tại"));

		User user = auth.getUser();

		validateUserStatus(user);

		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new RuntimeException("Tính năng quên mật khẩu chỉ áp dụng " + "cho tài khoản đã kích hoạt.");
		}

		return auth;
	}

	private UserAuthProvider getMyActiveLocalProvider(String idUser) {
		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Bạn chưa đăng nhập");
		}

		UserAuthProvider provider = authProviderRepository
				.findByUser_IdUserAndProviderAndDeletedAtIsNull(idUser, "local")
				.orElseThrow(() -> new RuntimeException("Tài khoản hiện tại không có email đăng nhập local"));

		User user = provider.getUser();

		validateUserStatus(user);

		if (provider.getEmail() == null || provider.getEmail().isBlank()) {
			throw new RuntimeException("Tài khoản local chưa có địa chỉ email");
		}

		return provider;
	}

	@Override
	public Map<String, Object> sendMyEmailOtp(String idUser) {
		UserAuthProvider provider = getMyActiveLocalProvider(idUser);

		if (provider.getEmailVerifiedAt() != null) {
			throw new RuntimeException("Email đã được xác thực");
		}

		String normalizedEmail = normalizeEmail(provider.getEmail());

		otpService.sendOtp(normalizedEmail, VERIFY_EMAIL_OTP);

		return Map.of("message", "Mã OTP đã được gửi đến email của bạn");
	}

	@Override
	@Transactional
	public Map<String, Object> verifyMyEmailOtp(String idUser, String otp) {
		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("Vui lòng nhập mã OTP");
		}

		UserAuthProvider provider = getMyActiveLocalProvider(idUser);

		if (provider.getEmailVerifiedAt() != null) {
			return Map.of("message", "Email đã được xác thực trước đó", "status", provider.getUser().getStatus());
		}

		String normalizedEmail = normalizeEmail(provider.getEmail());

		otpService.verifyOtp(normalizedEmail, VERIFY_EMAIL_OTP, otp.trim());

		LocalDateTime verifiedAt = LocalDateTime.now();

		provider.setEmailVerifiedAt(verifiedAt);
		provider.setUpdatedAt(verifiedAt);

		authProviderRepository.save(provider);

		User refreshedUser = userStatusService.refreshStatus(provider.getUser());

		otpService.clearOtp(normalizedEmail, VERIFY_EMAIL_OTP);

		return Map.of("message", "Xác thực email thành công", "emailVerifiedAt", verifiedAt, "status",
				refreshedUser.getStatus());
	}

	@Override
	@Transactional
	public AuthenticatedUser login(String email, String password) {

		String normalizedEmail = normalizeEmail(email);

		if (password == null || password.isBlank()) {
			throw new RuntimeException("Mật khẩu không được để trống");
		}

		UserAuthProvider auth = authProviderRepository
				.findByEmailAndProviderAndDeletedAtIsNull(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

		String storedPassword = auth.getPassword();

		if (!passwordService.matches(password, storedPassword)) {
			throw new RuntimeException("Email hoặc mật khẩu không đúng");
		}

		User user = auth.getUser();

		/*
		 * Chỉ trả trạng thái tài khoản sau khi mật khẩu đúng.
		 */
		validateUserStatus(user);

		if (passwordService.needsUpgrade(storedPassword)) {

			auth.setPassword(passwordService.encode(password));

			auth.setUpdatedAt(LocalDateTime.now());

			authProviderRepository.save(auth);
		}

		return createLoginResult(user);
	}

	@Override
	public Map<String, Object> sendOtp(String phone) {
		if (phone == null || phone.isBlank()) {
			throw new RuntimeException("Số điện thoại không được để trống");
		}

		String normalizedPhone = phone.trim();

		UserAuthProvider existingProvider = authProviderRepository.findByPhoneAndProvider(normalizedPhone, "phone")
				.orElse(null);

		if (existingProvider != null) {
			validateUserStatus(existingProvider.getUser());
		}

		otpService.sendOtp(normalizedPhone, "phone");

		return Map.of("message", "Đã gửi OTP");
	}

	@Override
	public AuthenticatedUser verifyPhoneOtp(String phone, String otp) {
		if (phone == null || phone.isBlank()) {
			throw new RuntimeException("Số điện thoại không được để trống");
		}

		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("OTP không được để trống");
		}

		String normalizedPhone = phone.trim();

		otpService.verifyOtp(normalizedPhone, "phone", otp.trim());

		UserAuthProvider provider = authProviderRepository.findByPhoneAndProvider(normalizedPhone, "phone")
				.orElse(null);

		User user;

		if (provider != null) {
			user = provider.getUser();

			validateUserStatus(user);

			provider.setDeletedAt(null);
			provider.setPhoneVerifiedAt(LocalDateTime.now());
			provider.setUpdatedAt(LocalDateTime.now());

			authProviderRepository.save(provider);
		} else {
			user = createPendingPhoneUser();

			UserAuthProvider newProvider = new UserAuthProvider();

			newProvider.setUser(user);
			newProvider.setProvider("phone");
			newProvider.setPhone(normalizedPhone);
			newProvider.setPhoneVerifiedAt(LocalDateTime.now());
			newProvider.setCreatedAt(LocalDateTime.now());
			newProvider.setUpdatedAt(LocalDateTime.now());
			newProvider.setDeletedAt(null);

			authProviderRepository.save(newProvider);
		}

		userStatusService.refreshStatus(user);

		otpService.clearOtp(normalizedPhone, "phone");

		return createLoginResult(user);
	}

	@Override
	@Transactional
	public Map<String, Object> register(String email, String password, String fullName, String gender,
			String birthday) {
		String normalizedEmail = normalizeEmail(email);

		passwordService.validateNewPassword(password);

		if (fullName == null || fullName.isBlank()) {
			throw new RuntimeException("Họ và tên không được để trống");
		}

		String normalizedFullName = fullName.trim();

		/*
		 * Kiểm tra email trên tất cả provider.
		 *
		 * Không được tự liên kết local vào tài khoản Google tại đây vì người đăng ký
		 * chưa chứng minh họ sở hữu tài khoản Google/email đó.
		 */
		List<UserAuthProvider> existingProviders = authProviderRepository.findAllByEmailIgnoreCase(normalizedEmail);

		UserAuthProvider activeProvider = existingProviders.stream().filter(provider -> provider.getDeletedAt() == null)
				.filter(provider -> provider.getUser() != null)
				.filter(provider -> !"delete".equalsIgnoreCase(provider.getUser().getStatus())).findFirst()
				.orElse(null);

		if (activeProvider != null) {
			if ("google".equalsIgnoreCase(activeProvider.getProvider())) {
				throw new RuntimeException(
						"Email này đã được đăng ký bằng Google. " + "Vui lòng sử dụng nút đăng nhập Google.");
			}

			throw new RuntimeException("Email đã tồn tại");
		}

		/*
		 * Email từng thuộc tài khoản bị xóa hoặc provider đã bị xóa mềm.
		 */
		if (!existingProviders.isEmpty()) {
			throw new RuntimeException("Email này thuộc tài khoản đã bị xóa. "
					+ "Nếu muốn khôi phục, vui lòng liên hệ hotline để được hỗ trợ.");
		}

		/*
		 * Gender không bắt buộc.
		 */
		String normalizedGender;

		if (gender == null || gender.isBlank()) {
			normalizedGender = "Unknown";
		} else {
			normalizedGender = gender.trim();

			boolean validGender = "Male".equalsIgnoreCase(normalizedGender)
					|| "Female".equalsIgnoreCase(normalizedGender) || "Unknown".equalsIgnoreCase(normalizedGender);

			if (!validGender) {
				throw new RuntimeException("Giới tính không hợp lệ");
			}

			if ("Male".equalsIgnoreCase(normalizedGender)) {
				normalizedGender = "Male";
			} else if ("Female".equalsIgnoreCase(normalizedGender)) {
				normalizedGender = "Female";
			} else {
				normalizedGender = "Unknown";
			}
		}

		/*
		 * Birthday không bắt buộc.
		 */
		LocalDate parsedBirthday = null;

		if (birthday != null && !birthday.isBlank()) {
			try {
				parsedBirthday = LocalDate.parse(birthday.trim());
			} catch (DateTimeParseException exception) {
				throw new RuntimeException("Ngày sinh không hợp lệ. Định dạng yêu cầu là yyyy-MM-dd");
			}

			if (parsedBirthday.isAfter(LocalDate.now())) {
				throw new RuntimeException("Ngày sinh không được lớn hơn ngày hiện tại");
			}
		}

		String idUser = generateRandom5Number();
		String prefixName = generateNameCode(normalizedFullName);

		String genderCode = switch (normalizedGender) {
		case "Male" -> "M";
		case "Female" -> "F";
		default -> "U";
		};

		String yearCode = parsedBirthday == null ? "00"
				: String.format(Locale.ROOT, "%02d", parsedBirthday.getYear() % 100);

		String userCode = prefixName + genderCode + yearCode + idUser;

		Role userRole = roleRepository.findById(3L).orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

		User user = new User();
		user.setIdUser(idUser);
		user.setUserCode(userCode);
		user.setFullName(normalizedFullName);
		user.setGender(normalizedGender);
		user.setBirthday(parsedBirthday);
		user.setStatus("pending");
		user.setRole(userRole);

		user = userRepository.save(user);

		UserAuthProvider localProvider = new UserAuthProvider();
		localProvider.setUser(user);
		localProvider.setProvider("local");
		localProvider.setEmail(normalizedEmail);
		localProvider.setEmailVerifiedAt(null);
		localProvider.setPassword(passwordService.encode(password));

		authProviderRepository.save(localProvider);

		return Map.of("message", "Đăng ký thành công");
	}

	@Override
	public Map<String, Object> forgotPassword(String email) {
		String normalizedEmail = normalizeEmail(email);

		getActiveLocalProviderForReset(normalizedEmail);

		otpService.sendOtp(normalizedEmail, RESET_PASSWORD_OTP);

		return Map.of("message", "Đã gửi OTP về email");
	}

	@Override
	public Map<String, Object> verifyForgotOtp(String email, String otp) {
		String normalizedEmail = normalizeEmail(email);

		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("OTP không được để trống");
		}

		/*
		 * Kiểm tra lại trạng thái vì tài khoản có thể đã bị khóa sau lúc gửi OTP.
		 */
		getActiveLocalProviderForReset(normalizedEmail);

		otpService.verifyOtp(normalizedEmail, RESET_PASSWORD_OTP, otp.trim());

		String resetToken = otpService.createVerificationToken(normalizedEmail, RESET_PASSWORD_OTP);

		otpService.clearOtp(normalizedEmail, RESET_PASSWORD_OTP);

		return Map.of("message", "Xác thực OTP thành công", "resetToken", resetToken);
	}

	@Override
	@Transactional
	public Map<String, Object> resetPassword(String email, String newPassword, String resetToken) {
		String normalizedEmail = normalizeEmail(email);

		passwordService.validateNewPassword(newPassword);

		/*
		 * Kiểm tra trạng thái lần cuối trước khi đổi mật khẩu.
		 */
		UserAuthProvider auth = getActiveLocalProviderForReset(normalizedEmail);

		boolean validResetToken = otpService.consumeVerificationToken(normalizedEmail, RESET_PASSWORD_OTP, resetToken);

		if (!validResetToken) {
			throw new RuntimeException("Phiên đổi mật khẩu không hợp lệ " + "hoặc đã hết hạn");
		}

		auth.setPassword(passwordService.encode(newPassword));

		auth.setUpdatedAt(LocalDateTime.now());

		/*
		 * Flush mật khẩu mới xuống database trước khi vô hiệu hóa các session.
		 */
		authProviderRepository.saveAndFlush(auth);

		/*
		 * Mọi JSESSIONID của người dùng sẽ bị đánh dấu hết hạn. Request tiếp theo từ
		 * các thiết bị cũ sẽ nhận HTTP 401.
		 */
		userSessionService.expireAllSessions(auth.getUser().getIdUser());

		return Map.of("message", "Đổi mật khẩu thành công. " + "Tất cả phiên đăng nhập cũ đã bị đăng xuất");
	}

	@Override
	@Transactional
	public AuthenticatedUser googleLogin(String credential) {
		if (credential == null || credential.isBlank()) {
			throw new RuntimeException("Google ID token không hợp lệ");
		}

		GoogleIdToken idToken;

		try {
			/*
			 * verify() kiểm tra:
			 *
			 * - Chữ ký của Google. - Issuer là Google. - Token chưa hết hạn. - aud đúng
			 * GOOGLE_CLIENT_ID.
			 */
			idToken = googleIdTokenVerifier.verify(credential.trim());
		} catch (GeneralSecurityException | IOException exception) {
			throw new RuntimeException("Không thể xác thực Google ID token", exception);
		}

		if (idToken == null) {
			/*
			 * Có thể do:
			 *
			 * - Token giả. - Token hết hạn. - Token cấp cho Client ID khác. - Chữ ký không
			 * hợp lệ.
			 */
			throw new RuntimeException("Google ID token không hợp lệ " + "hoặc không thuộc ứng dụng này");
		}

		GoogleIdToken.Payload payload = idToken.getPayload();

		String googleId = payload.getSubject();
		String email = payload.getEmail();
		Boolean emailVerified = payload.getEmailVerified();

		if (googleId == null || googleId.isBlank()) {
			throw new RuntimeException("Google user ID không hợp lệ");
		}

		if (email == null || email.isBlank()) {
			throw new RuntimeException("Google không cung cấp email");
		}

		if (!Boolean.TRUE.equals(emailVerified)) {
			throw new RuntimeException("Email Google chưa được xác thực");
		}

		String normalizedEmail = normalizeEmail(email);

		Object nameValue = payload.get("name");

		String fullName = nameValue instanceof String name && !name.isBlank() ? name.trim() : "Google User";

		/*
		 * Tìm tài khoản bằng Google sub. Không dùng email làm định danh Google.
		 */
		UserAuthProvider googleProvider = authProviderRepository.findByProviderIdAndProvider(googleId.trim(), "google")
				.orElse(null);

		if (googleProvider != null) {
			User user = googleProvider.getUser();

			validateUserStatus(user);

			/*
			 * Khôi phục liên kết Google nếu provider trước đó bị soft-delete.
			 */
			googleProvider.setDeletedAt(null);
			googleProvider.setEmail(normalizedEmail);

			/*
			 * Google đã xác thực email.
			 */
			if (googleProvider.getEmailVerifiedAt() == null) {
				googleProvider.setEmailVerifiedAt(LocalDateTime.now());
			}

			googleProvider.setUpdatedAt(LocalDateTime.now());

			authProviderRepository.save(googleProvider);

			userStatusService.refreshStatus(user);

			return createLoginResult(user);
		}

		/*
		 * Google provider chưa tồn tại. Kiểm tra tài khoản local cùng email.
		 */
		UserAuthProvider localProvider = authProviderRepository
				.findByEmailAndProviderAndDeletedAtIsNull(normalizedEmail, "local").orElse(null);

		User user;

		if (localProvider != null) {
			user = localProvider.getUser();

			validateUserStatus(user);

			/*
			 * Không tự liên kết Google với local chưa xác thực email.
			 *
			 * Người giữ mật khẩu local vẫn có thể truy cập tài khoản sau khi liên kết.
			 */
			if (localProvider.getEmailVerifiedAt() == null) {
				throw new RuntimeException("Email này đã được đăng ký bằng " + "tài khoản local nhưng chưa xác thực. "
						+ "Vui lòng đăng nhập bằng mật khẩu, " + "xác thực email rồi đăng nhập Google.");
			}
		} else {
			/*
			 * Tài khoản Google hoàn toàn mới. Google đã verified email nhưng tài khoản vẫn
			 * pending cho tới khi hoàn thiện hồ sơ và có ít nhất một địa chỉ.
			 */
			user = createPendingGoogleUser(fullName);
		}

		UserAuthProvider newGoogleProvider = new UserAuthProvider();

		newGoogleProvider.setUser(user);
		newGoogleProvider.setProvider("google");
		newGoogleProvider.setProviderId(googleId.trim());
		newGoogleProvider.setEmail(normalizedEmail);
		newGoogleProvider.setPassword(null);
		newGoogleProvider.setEmailVerifiedAt(LocalDateTime.now());
		newGoogleProvider.setPhoneVerifiedAt(null);
		newGoogleProvider.setDeletedAt(null);

		authProviderRepository.save(newGoogleProvider);

		userStatusService.refreshStatus(user);

		return createLoginResult(user);
	}
	
	@Override
	@Transactional
	public Map<String, Object> removeMyEmailVerification(
			String idUser
	) {
		UserAuthProvider provider =
				getMyActiveLocalProvider(idUser);

		String normalizedEmail =
				normalizeEmail(provider.getEmail());

		/*
		 * Hủy luôn OTP xác thực đang tồn tại.
		 */
		otpService.clearOtp(
				normalizedEmail,
				VERIFY_EMAIL_OTP
		);

		if (provider.getEmailVerifiedAt() == null) {
			return Map.of(
					"message", "Email hiện chưa được xác thực",
					"status", provider.getUser().getStatus()
			);
		}

		LocalDateTime now = LocalDateTime.now();

		provider.setEmailVerifiedAt(null);
		provider.setUpdatedAt(now);

		authProviderRepository.save(provider);

		User refreshedUser =
				userStatusService.refreshStatus(
						provider.getUser()
				);

		return Map.of(
				"message", "Đã bỏ xác thực email",
				"status", refreshedUser.getStatus()
		);
	}
}