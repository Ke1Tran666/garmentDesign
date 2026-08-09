package com.garmentDesign.service.Impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserAuthProviderRepository authProviderRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final OtpService otpService;
	private final PasswordService passwordService;
	private final UserStatusService userStatusService;

	private static final String VERIFY_EMAIL_OTP = "verify-email";
	private static final String RESET_PASSWORD_OTP = "reset-password";

	public AuthServiceImpl(UserAuthProviderRepository authProviderRepository, UserRepository userRepository,
			RoleRepository roleRepository, OtpService otpService, PasswordService passwordService,
			UserStatusService userStatusService) {
		this.authProviderRepository = authProviderRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.otpService = otpService;
		this.passwordService = passwordService;
		this.userStatusService = userStatusService;
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

		User user = auth.getUser();

		/*
		 * Chỉ chặn inactive, banned và delete. pending vẫn được đăng nhập.
		 */
		validateUserStatus(user);

		String storedPassword = auth.getPassword();

		if (!passwordService.matches(password, storedPassword)) {

			throw new RuntimeException("Email hoặc mật khẩu không đúng");
		}

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
	public Map<String, Object> sendEmailOtp(String email) {
		String normalizedEmail = normalizeEmail(email);

		authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản local với email này"));

		otpService.sendOtp(normalizedEmail, VERIFY_EMAIL_OTP);

		return Map.of("message", "Đã gửi OTP xác thực email");
	}

	@Override
	@Transactional
	public Map<String, Object> verifyEmailOtp(String email, String otp) {

		String normalizedEmail = normalizeEmail(email);

		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("OTP không được để trống");
		}

		UserAuthProvider provider = authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản local"));

		otpService.verifyOtp(normalizedEmail, VERIFY_EMAIL_OTP, otp.trim());

		User user = provider.getUser();

		validateUserStatus(user);

		provider.setDeletedAt(null);
		provider.setEmailVerifiedAt(LocalDateTime.now());
		provider.setUpdatedAt(LocalDateTime.now());

		authProviderRepository.save(provider);
		
		userStatusService.refreshStatus(user);

		otpService.clearOtp(normalizedEmail, VERIFY_EMAIL_OTP);

		return Map.of("message", "Xác thực email thành công");
	}

	@Override
	@Transactional
	public Map<String, Object> register(String email, String password, String fullName, String gender,
			String birthday) {

		String normalizedEmail = normalizeEmail(email);

		passwordService.validateNewPassword(password);

		UserAuthProvider existingAuth = authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElse(null);

		if (existingAuth != null) {
			User existingUser = existingAuth.getUser();

			if ("delete".equalsIgnoreCase(existingUser.getStatus())) {
				throw new RuntimeException(
						"Email này thuộc tài khoản đã bị xóa. Nếu muốn khôi phục vui lòng liên hệ hotline để được hỗ trợ.");
			}

			throw new RuntimeException("Email đã tồn tại");
		}

		String idUser = generateRandom5Number();
		String prefixName = generateNameCode(fullName);

		String genderCode;

		switch (gender.toLowerCase()) {
		case "male":
			genderCode = "M";
			break;
		case "female":
			genderCode = "F";
			break;
		default:
			genderCode = "U";
			break;
		}

		String yearCode = "00";

		if (birthday != null && !birthday.isEmpty()) {
			yearCode = birthday.substring(2, 4);
		}

		String userCode = prefixName + genderCode + yearCode + idUser;

		Role userRole = roleRepository.findById(3L).orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

		User user = new User();
		user.setIdUser(idUser);
		user.setUserCode(userCode);
		user.setFullName(fullName);
		user.setGender(gender);

		if (birthday != null && !birthday.isEmpty()) {
			user.setBirthday(LocalDate.parse(birthday));
		}

		user.setStatus("pending");
		user.setRole(userRole);

		user = userRepository.save(user);

		UserAuthProvider auth = new UserAuthProvider();
		auth.setUser(user);
		auth.setProvider("local");
		auth.setEmail(normalizedEmail);
		auth.setEmailVerifiedAt(null);
		auth.setPassword(passwordService.encode(password));

		authProviderRepository.save(auth);

		Map<String, Object> result = new HashMap<>();
		result.put("message", "Đăng ký thành công");

		return result;
	}

	@Override
	public Map<String, Object> forgotPassword(String email) {

		String normalizedEmail = normalizeEmail(email);

		UserAuthProvider auth = authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Email không tồn tại"));

		User user = auth.getUser();

		validateUserStatus(user);

		if (!"active".equalsIgnoreCase(user.getStatus())) {

			throw new RuntimeException("Tính năng quên mật khẩu chỉ áp dụng " + "cho tài khoản đã kích hoạt.");
		}

		otpService.sendOtp(normalizedEmail, RESET_PASSWORD_OTP);

		return Map.of("message", "Đã gửi OTP về email");
	}

	@Override
	public Map<String, Object> verifyForgotOtp(String email, String otp) {

		String normalizedEmail = normalizeEmail(email);

		if (otp == null || otp.isBlank()) {
			throw new RuntimeException("OTP không được để trống");
		}

		authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Email không tồn tại"));

		otpService.verifyOtp(normalizedEmail, RESET_PASSWORD_OTP, otp.trim());

		otpService.markVerified(normalizedEmail, RESET_PASSWORD_OTP);

		otpService.clearOtp(normalizedEmail, RESET_PASSWORD_OTP);

		return Map.of("message", "Xác thực OTP thành công");
	}

	@Override
	@Transactional
	public Map<String, Object> resetPassword(String email, String newPassword) {

		String normalizedEmail = normalizeEmail(email);

		passwordService.validateNewPassword(newPassword);

		if (!otpService.isVerified(normalizedEmail, RESET_PASSWORD_OTP)) {

			throw new RuntimeException("Bạn chưa xác thực OTP hoặc phiên đã hết hạn");
		}

		UserAuthProvider auth = authProviderRepository.findByEmailAndProvider(normalizedEmail, "local")
				.orElseThrow(() -> new RuntimeException("Email không tồn tại"));

		auth.setPassword(passwordService.encode(newPassword));

		auth.setUpdatedAt(LocalDateTime.now());

		authProviderRepository.save(auth);

		otpService.clearVerified(normalizedEmail, RESET_PASSWORD_OTP);

		return Map.of("message", "Đổi mật khẩu thành công");
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

	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public AuthenticatedUser googleLogin(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new RuntimeException("Google access token không hợp lệ");
		}

		Map<String, Object> googleUser;

		try {
			RestTemplate restTemplate = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken.trim());

			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

			ResponseEntity<Map> response = restTemplate.exchange("https://www.googleapis.com/oauth2/v3/userinfo",
					HttpMethod.GET, requestEntity, Map.class);

			googleUser = response.getBody();
		} catch (RestClientException exception) {
			throw new RuntimeException("Không thể xác thực tài khoản Google", exception);
		}

		if (googleUser == null) {
			throw new RuntimeException("Không lấy được thông tin tài khoản Google");
		}

		Object subValue = googleUser.get("sub");
		Object emailValue = googleUser.get("email");
		Object verifiedValue = googleUser.get("email_verified");

		if (subValue == null || emailValue == null || verifiedValue == null) {
			throw new RuntimeException("Thông tin Google không đầy đủ");
		}

		String googleId = subValue.toString().trim();
		String normalizedEmail = normalizeEmail(emailValue.toString());

		boolean emailVerified = Boolean.TRUE.equals(verifiedValue) || "true".equalsIgnoreCase(verifiedValue.toString());

		if (googleId.isEmpty()) {
			throw new RuntimeException("Google user ID không hợp lệ");
		}

		if (!emailVerified) {
			throw new RuntimeException("Email Google chưa được xác thực");
		}

		String fullName = googleUser.get("name") != null && !googleUser.get("name").toString().isBlank()
				? googleUser.get("name").toString().trim()
				: "Google User";

		/*
		 * 1. Tìm theo Google sub.
		 *
		 * Email Google có thể thay đổi, nhưng sub là định danh ổn định của tài khoản
		 * Google.
		 */
		UserAuthProvider googleProvider = authProviderRepository.findByProviderIdAndProvider(googleId, "google")
				.orElse(null);

		if (googleProvider != null) {
			User user = googleProvider.getUser();

			validateUserStatus(user);

			/*
			 * Cho phép khôi phục provider đã soft-delete khi chính chủ đăng nhập lại bằng
			 * Google.
			 */
			googleProvider.setDeletedAt(null);
			googleProvider.setEmail(normalizedEmail);

			if (googleProvider.getEmailVerifiedAt() == null) {
				googleProvider.setEmailVerifiedAt(LocalDateTime.now());
			}

			googleProvider.setUpdatedAt(LocalDateTime.now());

			authProviderRepository.save(googleProvider);

			userStatusService.refreshStatus(user);

			return createLoginResult(user);
		}

		/*
		 * 2. Chưa có Google provider: kiểm tra tài khoản local cùng email.
		 */
		UserAuthProvider localProvider = authProviderRepository
				.findByEmailAndProviderAndDeletedAtIsNull(normalizedEmail, "local").orElse(null);

		User user;

		if (localProvider != null) {
			user = localProvider.getUser();

			validateUserStatus(user);

			/*
			 * Không tự động liên kết Google vào một tài khoản local chưa xác thực email.
			 *
			 * Nếu tự liên kết, người đang giữ mật khẩu local vẫn có thể đăng nhập vào tài
			 * khoản vừa được chủ email xác thực bằng Google.
			 */
			if (localProvider.getEmailVerifiedAt() == null) {
				throw new RuntimeException("Email này đã được đăng ký bằng tài khoản local "
						+ "nhưng chưa xác thực. Vui lòng đăng nhập bằng "
						+ "mật khẩu và xác thực email trước khi liên kết Google.");
			}
		} else {
			/*
			 * 3. Người dùng Google hoàn toàn mới.
			 */
			user = createPendingGoogleUser(fullName);
		}

		/*
		 * 4. Tạo provider Google.
		 *
		 * Google đã xác thực email nên emailVerifiedAt có dữ liệu ngay, không gửi OTP.
		 */
		UserAuthProvider newGoogleProvider = new UserAuthProvider();

		newGoogleProvider.setUser(user);
		newGoogleProvider.setProvider("google");
		newGoogleProvider.setProviderId(googleId);
		newGoogleProvider.setEmail(normalizedEmail);
		newGoogleProvider.setPassword(null);
		newGoogleProvider.setEmailVerifiedAt(LocalDateTime.now());
		newGoogleProvider.setPhoneVerifiedAt(null);
		newGoogleProvider.setDeletedAt(null);

		authProviderRepository.save(newGoogleProvider);

		userStatusService.refreshStatus(user);

		return createLoginResult(user);
	}
}