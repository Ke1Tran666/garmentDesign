package com.garmentDesign.service.Impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.garmentDesign.dto.user.UpdateProfileRequest;
import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.PasswordService;
import com.garmentDesign.service.UserService;
import com.garmentDesign.service.UserStatusService;
import com.garmentDesign.service.UserSessionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository repository;
	private final UserAuthProviderRepository authProviderRepository;
	private final UserAddressRepository addressRepository;
	private final PasswordService passwordService;
	private final UserStatusService userStatusService;
	private final UserSessionService userSessionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

	private static final int MAX_AVATAR_DIMENSION = 4096;

	private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

	private final Path uploadRoot;
	private final String publicBaseUrl;

	public UserServiceImpl(UserRepository repository, UserAuthProviderRepository authProviderRepository,
			UserAddressRepository addressRepository, PasswordService passwordService,
			UserStatusService userStatusService, UserSessionService userSessionService,
			@Value("${app.upload.root-dir:uploads}") String uploadRoot,
			@Value("${app.public-base-url:http://localhost:8082}") String publicBaseUrl) {

		this.repository = repository;
		this.authProviderRepository = authProviderRepository;
		this.addressRepository = addressRepository;
		this.passwordService = passwordService;
		this.userStatusService = userStatusService;
		this.userSessionService = userSessionService;

		this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();

		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			throw new IllegalStateException("APP_PUBLIC_BASE_URL chưa được cấu hình");
		}

		this.publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
	}

	private void createUnverifiedPhoneProvider(User user, String normalizedPhone) {

		UserAuthProvider provider = new UserAuthProvider();

		provider.setUser(user);
		provider.setProvider("phone");
		provider.setPhone(normalizedPhone);
		provider.setPhoneVerifiedAt(null);
		provider.setDeletedAt(null);

		authProviderRepository.save(provider);
	}

	private void removeCurrentPhoneForReplacement(UserAuthProvider provider) {

		if (provider.getPhoneVerifiedAt() != null) {
			/*
			 * Số đã xác thực: giữ lịch sử.
			 */
			LocalDateTime now = LocalDateTime.now();

			provider.setDeletedAt(now);
			provider.setUpdatedAt(now);

			authProviderRepository.save(provider);
		} else {
			/*
			 * Số chưa xác thực: xóa hoàn toàn.
			 */
			authProviderRepository.delete(provider);
			authProviderRepository.flush();
		}
	}

	private String validateAvatarAndGetExtension(MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw new RuntimeException("Vui lòng chọn ảnh đại diện");
		}

		if (file.getSize() > MAX_AVATAR_SIZE) {
			throw new RuntimeException("Ảnh đại diện không được vượt quá 5 MB");
		}

		String contentType = file.getContentType();

		if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {

			throw new RuntimeException("Chỉ chấp nhận ảnh JPEG hoặc PNG");
		}

		try (InputStream inputStream = file.getInputStream();
				ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
			if (imageInputStream == null) {
				throw new RuntimeException("File ảnh không hợp lệ");
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

			if (!readers.hasNext()) {
				throw new RuntimeException("File tải lên không phải ảnh hợp lệ");
			}

			ImageReader reader = readers.next();

			try {
				reader.setInput(imageInputStream, true, true);

				int width = reader.getWidth(0);
				int height = reader.getHeight(0);

				if (width <= 0 || height <= 0) {
					throw new RuntimeException("Kích thước ảnh không hợp lệ");
				}

				if (width > MAX_AVATAR_DIMENSION || height > MAX_AVATAR_DIMENSION) {

					throw new RuntimeException("Chiều rộng và chiều cao ảnh " + "không được vượt quá 4096 pixel");
				}

				String format = reader.getFormatName().toLowerCase(Locale.ROOT);

				return switch (format) {
				case "jpg", "jpeg" -> ".jpg";
				case "png" -> ".png";
				default -> throw new RuntimeException("Chỉ chấp nhận ảnh JPEG hoặc PNG");
				};
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			throw new RuntimeException("Không thể đọc file ảnh", exception);
		}
	}

	private void deleteStoredAvatarFile(String avatarUrl) {

		if (avatarUrl == null || avatarUrl.isBlank()) {

			return;
		}

		String normalizedUrl = avatarUrl.replace('\\', '/');

		String marker = "/uploads/";

		int markerIndex = normalizedUrl.indexOf(marker);

		if (markerIndex < 0) {
			return;
		}

		String relativePath = normalizedUrl.substring(markerIndex + marker.length());

		if (relativePath.isBlank()) {
			return;
		}

		Path filePath = uploadRoot.resolve(relativePath).normalize();

		if (!filePath.startsWith(uploadRoot)) {
			return;
		}

		try {
			Files.deleteIfExists(filePath);

			Path avatarFolder = filePath.getParent();

			/*
			 * Xóa thư mục avatar nếu đã rỗng.
			 */
			if (avatarFolder != null && avatarFolder.startsWith(uploadRoot)) {

				Files.deleteIfExists(avatarFolder);
			}
		} catch (IOException exception) {
			LOGGER.warn("Không thể xóa avatar cũ: {}", filePath, exception);
		}
	}

	private String generateUserCode(String fullName, String gender, LocalDate birthday, String oldUserCode,
			String idUser) {
		String nameCode = getNameCode(fullName);
		String genderCode = getGenderCode(gender);
		String yearCode = getYearCode(birthday);
		String suffixCode = getSuffixCode(oldUserCode, idUser);

		return nameCode + genderCode + yearCode + suffixCode;
	}

	private String getStorageCode(User user) {
		if (user == null || user.getUserCode() == null) {

			throw new RuntimeException("Không thể xác định mã lưu trữ của người dùng");
		}

		String userCode = user.getUserCode().trim();

		if (userCode.length() < 5) {
			throw new RuntimeException("Mã người dùng không hợp lệ");
		}

		String storageCode = userCode.substring(userCode.length() - 5);

		if (!storageCode.matches("[A-Za-z0-9]{5}")) {
			throw new RuntimeException("Mã lưu trữ của người dùng không hợp lệ");
		}

		return storageCode;
	}

	private String getNameCode(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			return "USE";
		}

		String normalized = java.text.Normalizer.normalize(fullName.trim(), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").replaceAll("đ", "d").replaceAll("Đ", "D");

		String[] parts = normalized.trim().split("\\s+");

		if (parts.length == 0) {
			return "USE";
		}

		String lastName = parts[parts.length - 1].replaceAll("[^a-zA-Z]", "").toUpperCase();

		if (lastName.isEmpty()) {
			return "USE";
		}

		if (lastName.length() >= 3) {
			return lastName.substring(0, 3);
		}

		return String.format("%-3s", lastName).replace(' ', 'X');
	}

	private String getGenderCode(String gender) {
		if (gender == null) {
			return "U";
		}

		return switch (gender) {
		case "Male" -> "M";
		case "Female" -> "F";
		default -> "U";
		};
	}

	private String getYearCode(LocalDate birthday) {
		if (birthday == null) {
			return "01";
		}

		return String.format("%02d", birthday.getYear() % 100);
	}

	private String getSuffixCode(String oldUserCode, String idUser) {

		if (oldUserCode != null) {
			String normalized = oldUserCode.trim();

			if (normalized.length() >= 5) {
				return normalized.substring(normalized.length() - 5);
			}
		}

		/*
		 * Tương thích tài khoản cũ: năm số idUser hiện tại trở thành mã cố định.
		 */
		if (idUser != null) {
			String normalizedId = idUser.trim();

			if (normalizedId.length() >= 5) {
				return normalizedId.substring(normalizedId.length() - 5);
			}
		}

		throw new RuntimeException("Không thể xác định mã cố định của người dùng");
	}

	private String normalizeProfilePhone(String phone) {

		if (phone == null || phone.isBlank()) {
			return null;
		}

		String normalized = phone.trim().replace(" ", "").replace("-", "").replace(".", "");

		if (normalized.startsWith("+84")) {
			normalized = "0" + normalized.substring(3);
		} else if (normalized.startsWith("84") && normalized.length() == 11) {

			normalized = "0" + normalized.substring(2);
		}

		if (!normalized.matches("^0\\d{9}$")) {
			throw new RuntimeException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0");
		}

		return normalized;
	}

	private void updateProfilePhone(User user, String requestedPhone) {

		String normalizedPhone = normalizeProfilePhone(requestedPhone);

		/*
		 * Không nhập phone thì không thay đổi dữ liệu. Xóa phone sử dụng endpoint
		 * riêng.
		 */
		if (normalizedPhone == null) {
			return;
		}

		UserAuthProvider matchedProvider = authProviderRepository.findByPhoneAndProvider(normalizedPhone, "phone")
				.orElse(null);

		/*
		 * Số đã hoặc đang thuộc người dùng khác. Kể cả bản ghi đã soft-delete vẫn không
		 * tự ý chuyển quyền sở hữu sang tài khoản hiện tại.
		 */
		if (matchedProvider != null && matchedProvider.getUser() != null
				&& !user.getIdUser().equals(matchedProvider.getUser().getIdUser())) {

			throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác");
		}

		UserAuthProvider currentProvider = authProviderRepository
				.findByUser_IdUserAndProviderAndDeletedAtIsNull(user.getIdUser(), "phone").orElse(null);

		/*
		 * Số này đang hoạt động trên chính tài khoản.
		 */
		if (matchedProvider != null && matchedProvider.getDeletedAt() == null) {

			/*
			 * Trạng thái hiện tại đã đúng, không cần cập nhật hoặc xóa xác thực.
			 */
			if (normalizedPhone.equals(matchedProvider.getPhone())) {
				return;
			}
		}

		/*
		 * Số trùng với bản ghi đã soft-delete của chính người dùng.
		 */
		if (matchedProvider != null && matchedProvider.getDeletedAt() != null) {

			/*
			 * Trước khi khôi phục số cũ, xử lý số đang hoạt động hiện tại nếu đó là bản ghi
			 * khác.
			 */
			if (currentProvider != null && !currentProvider.getId().equals(matchedProvider.getId())) {

				removeCurrentPhoneForReplacement(currentProvider);
			}

			LocalDateTime now = LocalDateTime.now();

			matchedProvider.setDeletedAt(null);

			/*
			 * Theo yêu cầu: thêm lại số cũ thì phải xác thực lại từ đầu.
			 */
			matchedProvider.setPhoneVerifiedAt(null);
			matchedProvider.setUpdatedAt(now);

			authProviderRepository.save(matchedProvider);

			return;
		}

		/*
		 * Chưa có provider phone nào.
		 */
		if (currentProvider == null) {
			createUnverifiedPhoneProvider(user, normalizedPhone);

			return;
		}

		/*
		 * Người dùng gửi lại đúng số đang có.
		 */
		if (normalizedPhone.equals(currentProvider.getPhone())) {
			return;
		}

		/*
		 * Số hiện tại đã xác thực: giữ lịch sử bằng soft-delete và tạo record mới.
		 */
		if (currentProvider.getPhoneVerifiedAt() != null) {
			removeCurrentPhoneForReplacement(currentProvider);

			createUnverifiedPhoneProvider(user, normalizedPhone);

			return;
		}

		/*
		 * Số hiện tại chưa xác thực: có thể cập nhật trực tiếp vì chưa cần giữ lịch sử.
		 */
		currentProvider.setPhone(normalizedPhone);
		currentProvider.setPhoneVerifiedAt(null);
		currentProvider.setDeletedAt(null);
		currentProvider.setUpdatedAt(LocalDateTime.now());

		authProviderRepository.save(currentProvider);
	}

	@Override
	public List<User> findAll() {
		return repository.findAll();
	}

	@Override
	public Map<String, Object> getProfile(String idUser) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		Map<String, Object> profile = new HashMap<>();

		profile.put("user", user);

		List<Map<String, Object>> safeProviders = authProviderRepository.findByUser_IdUserAndDeletedAtIsNull(idUser)
				.stream().map(provider -> {
					Map<String, Object> item = new HashMap<>();

					item.put("id", provider.getId());

					item.put("provider", provider.getProvider());

					item.put("email", provider.getEmail());

					item.put("phone", provider.getPhone());

					item.put("emailVerifiedAt", provider.getEmailVerifiedAt());

					item.put("phoneVerifiedAt", provider.getPhoneVerifiedAt());

					return item;
				}).toList();

		profile.put("authProviders", safeProviders);

		profile.put("addresses", addressRepository.findByUser_IdUserAndDeletedAtIsNull(idUser));

		profile.put("defaultAddress", user.getDefaultAddress());

		return profile;
	}

	@Override
	public User findById(String id) {
		return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
	}

	@Override
	public User save(User data) {
		return repository.save(data);
	}

	@Override
	@Transactional
	public User updateProfile(String id, UpdateProfileRequest request) {

		if (request == null) {
			throw new RuntimeException("Thông tin cập nhật không hợp lệ");
		}

		User user = findById(id);

		String fullName = request.getFullName() == null ? "" : request.getFullName().trim();

		String gender = request.getGender() == null ? "" : request.getGender().trim();

		LocalDate birthday = request.getBirthday();

		if (fullName.isBlank()) {
			throw new RuntimeException("Vui lòng nhập họ và tên");
		}

		if (birthday == null) {
			throw new RuntimeException("Vui lòng nhập ngày sinh");
		}

		if (!birthday.isBefore(LocalDate.now())) {
			throw new RuntimeException("Ngày sinh phải nhỏ hơn ngày hiện tại");
		}

		boolean validGender = "Male".equalsIgnoreCase(gender) || "Female".equalsIgnoreCase(gender)
				|| "Unknown".equalsIgnoreCase(gender);

		if (!validGender) {
			throw new RuntimeException("Giới tính không hợp lệ");
		}

		String normalizedGender;

		if ("Male".equalsIgnoreCase(gender)) {
			normalizedGender = "Male";
		} else if ("Female".equalsIgnoreCase(gender)) {
			normalizedGender = "Female";
		} else {
			normalizedGender = "Unknown";
		}

		user.setFullName(fullName);
		user.setGender(normalizedGender);
		user.setBirthday(birthday);

		updateProfilePhone(user, request.getPhone());

		String newUserCode = generateUserCode(user.getFullName(), user.getGender(), user.getBirthday(),
				user.getUserCode(), user.getIdUser());

		user.setUserCode(newUserCode);
		user.setUpdatedAt(LocalDateTime.now());

		repository.save(user);

		return userStatusService.refreshStatus(user);
	}

	@Override
	@Transactional
	public void delete(String id) {
		deleteAccount(id);
	}

	@Override
	@Transactional
	public Map<String, Object> deletePhone(String idUser, Long providerId) {

		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Bạn chưa đăng nhập");
		}

		if (providerId == null) {
			throw new RuntimeException("Thông tin số điện thoại không hợp lệ");
		}

		UserAuthProvider provider = authProviderRepository
				.findByIdAndUser_IdUserAndProviderAndDeletedAtIsNull(providerId, idUser, "phone")
				.orElseThrow(() -> new RuntimeException("Không tìm thấy số điện thoại"));

		User user = provider.getUser();

		boolean verified = provider.getPhoneVerifiedAt() != null;

		if (verified) {
			/*
			 * Số đã xác thực: soft-delete để giữ lịch sử.
			 */
			LocalDateTime now = LocalDateTime.now();

			provider.setDeletedAt(now);
			provider.setUpdatedAt(now);

			authProviderRepository.save(provider);
		} else {
			/*
			 * Số chưa xác thực: xóa hoàn toàn.
			 */
			authProviderRepository.delete(provider);
			authProviderRepository.flush();
		}

		/*
		 * Việc xóa số đã xác thực có thể làm tài khoản chuyển từ active về pending.
		 */
		User refreshedUser = userStatusService.refreshStatus(user);

		return Map.of("message", verified ? "Đã gỡ số điện thoại" : "Đã xóa số điện thoại", "deleteType",
				verified ? "soft" : "hard", "status", refreshedUser.getStatus());
	}

	// Delete Avatar
	@Override
	@Transactional
	public Map<String, Object> deleteAvatar(String idUser) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		String oldAvatar = user.getAvatar();

		user.setAvatar(null);
		user.setUpdatedAt(LocalDateTime.now());

		repository.saveAndFlush(user);

		deleteStoredAvatarFile(oldAvatar);

		Map<String, Object> result = new HashMap<>();

		result.put("message", "Xóa avatar thành công");
		result.put("avatar", null);

		return result;
	}

	// Upload Avatar
	@Override
	@Transactional
	public Map<String, Object> uploadAvatar(String idUser, MultipartFile file) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		String extension = validateAvatarAndGetExtension(file);

		String storageCode = getStorageCode(user);

		Path avatarDirectory = uploadRoot.resolve(storageCode).resolve("avatar").normalize();

		if (!avatarDirectory.startsWith(uploadRoot)) {
			throw new RuntimeException("Đường dẫn lưu avatar không hợp lệ");
		}

		try {
			Files.createDirectories(avatarDirectory);
		} catch (IOException exception) {
			throw new RuntimeException("Không thể tạo thư mục lưu avatar", exception);
		}

		String fileName = UUID.randomUUID() + extension;

		Path filePath = avatarDirectory.resolve(fileName).normalize();

		if (!filePath.startsWith(avatarDirectory)) {
			throw new RuntimeException("Đường dẫn lưu avatar không hợp lệ");
		}

		try (InputStream inputStream = file.getInputStream()) {

			Files.copy(inputStream, filePath);
		} catch (IOException exception) {
			throw new RuntimeException("Không thể lưu ảnh đại diện", exception);
		}

		String oldAvatar = user.getAvatar();

		String avatarUrl = publicBaseUrl + "/uploads/" + storageCode + "/avatar/" + fileName;

		try {
			user.setAvatar(avatarUrl);
			user.setUpdatedAt(LocalDateTime.now());

			repository.saveAndFlush(user);
		} catch (RuntimeException exception) {
			try {
				Files.deleteIfExists(filePath);
			} catch (IOException cleanupException) {
				LOGGER.warn("Không thể dọn avatar sau khi lưu DB thất bại", cleanupException);
			}

			throw exception;
		}

		deleteStoredAvatarFile(oldAvatar);

		return Map.of("message", "Upload avatar thành công", "avatar", avatarUrl);
	}

	// Change Password
	@Override
	@Transactional
	public Map<String, Object> changePassword(String idUser, String oldPassword, String newPassword,
			String currentSessionId) {

		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Không tìm thấy người dùng");
		}

		if (currentSessionId == null || currentSessionId.isBlank()) {

			throw new RuntimeException("Không thể xác định phiên đăng nhập hiện tại");
		}

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new RuntimeException("Tính năng đổi mật khẩu chỉ áp dụng " + "cho tài khoản đã kích hoạt.");
		}

		if (oldPassword == null || oldPassword.isBlank()) {
			throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại");
		}

		passwordService.validateNewPassword(newPassword);

		if (oldPassword.equals(newPassword)) {
			throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
		}

		UserAuthProvider localProvider = authProviderRepository
				.findByUser_IdUserAndProviderAndDeletedAtIsNull(idUser, "local").orElseThrow(
						() -> new RuntimeException("Tài khoản của bạn chưa liên kết " + "đăng nhập bằng mật khẩu."));

		String storedPassword = localProvider.getPassword();

		if (storedPassword == null || storedPassword.isBlank()) {
			throw new RuntimeException("Tài khoản local chưa có mật khẩu.");
		}

		if (!passwordService.matches(oldPassword, storedPassword)) {

			throw new RuntimeException("Mật khẩu hiện tại không đúng");
		}

		localProvider.setPassword(passwordService.encode(newPassword));

		localProvider.setUpdatedAt(LocalDateTime.now());

		/*
		 * Flush mật khẩu mới xuống database trước khi vô hiệu hóa session của các thiết
		 * bị khác.
		 */
		authProviderRepository.saveAndFlush(localProvider);

		/*
		 * Không expire session đang thực hiện đổi mật khẩu.
		 */
		userSessionService.expireOtherSessions(idUser, currentSessionId);

		return Map.of("message", "Đổi mật khẩu thành công. " + "Các thiết bị khác đã được đăng xuất");
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> exportUserData(String idUser) {

		User user = repository.findById(idUser)
				.orElseThrow(() -> new RuntimeException(
						"Không tìm thấy người dùng"));

		Long defaultAddressId = user.getDefaultAddress() != null
				? user.getDefaultAddress().getAddressId()
				: null;

		List<Map<String, Object>> addresses = addressRepository
				.findByUser_IdUserOrderByCreatedAtAsc(idUser)
				.stream()
				.map(address -> toExportAddress(
						address,
						idUser,
						defaultAddressId))
				.toList();

		List<Map<String, Object>> authProviders = authProviderRepository
				.findByUser_IdUserOrderByCreatedAtAsc(idUser)
				.stream()
				.map(provider -> toExportAuthProvider(
						provider,
						idUser))
				.toList();

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("user", toExportUser(user, defaultAddressId));
		result.put("addresses", addresses);
		result.put("authProviders", authProviders);

		return result;
	}

	private Map<String, Object> toExportUser(
			User user,
			Long defaultAddressId) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("idUser", user.getIdUser());
		data.put("userCode", user.getUserCode());
		data.put("fullName", user.getFullName());
		data.put("avatar", user.getAvatar());
		data.put("gender", user.getGender());
		data.put("birthday", user.getBirthday());

		data.put(
				"roleId",
				user.getRole() != null
						? user.getRole().getIdRole()
						: null);

		data.put(
				"roleName",
				user.getRole() != null
						? user.getRole().getNameRole()
						: null);

		data.put("defaultAddressId", defaultAddressId);
		data.put("status", user.getStatus());
		data.put("lastLogin", user.getLastLogin());
		data.put("createdAt", user.getCreatedAt());
		data.put("updatedAt", user.getUpdatedAt());
		data.put("deletedAt", user.getDeletedAt());

		return data;
	}

	private Map<String, Object> toExportAddress(
			UserAddress address,
			String idUser,
			Long defaultAddressId) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("addressId", address.getAddressId());
		data.put("idUser", idUser);
		data.put("companyName", address.getCompanyName());
		data.put("address", address.getAddress());
		data.put("note", address.getNote());

		data.put(
				"isDefault",
				defaultAddressId != null
						&& defaultAddressId.equals(
								address.getAddressId()));

		data.put("createdAt", address.getCreatedAt());
		data.put("updatedAt", address.getUpdatedAt());
		data.put("deletedAt", address.getDeletedAt());

		return data;
	}

	private Map<String, Object> toExportAuthProvider(
			UserAuthProvider provider,
			String idUser) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("id", provider.getId());
		data.put("idUser", idUser);
		data.put("provider", provider.getProvider());
		data.put("email", provider.getEmail());
		data.put("phone", provider.getPhone());
		data.put("providerId", provider.getProviderId());
		data.put("emailVerifiedAt", provider.getEmailVerifiedAt());
		data.put("phoneVerifiedAt", provider.getPhoneVerifiedAt());
		data.put("createdAt", provider.getCreatedAt());
		data.put("updatedAt", provider.getUpdatedAt());
		data.put("deletedAt", provider.getDeletedAt());

		return data;
	}

	@Override
	@Transactional
	public Map<String, Object> deleteAccount(String idUser) {

		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Bạn chưa đăng nhập");
		}

		User user = repository
				.findByIdUserAndDeletedAtIsNull(idUser)
				.orElseThrow(() -> new RuntimeException(
						"Tài khoản không tồn tại hoặc đã được xóa"));

		LocalDateTime now = LocalDateTime.now();

		user.setDeletedAt(now);
		user.setUpdatedAt(now);
		user.setStatus("delete");

		List<UserAuthProvider> authProviders =
				authProviderRepository
						.findByUser_IdUserAndDeletedAtIsNull(idUser);

		authProviders.forEach(provider -> {
			provider.setDeletedAt(now);
			provider.setUpdatedAt(now);
		});

		authProviderRepository.saveAll(authProviders);
		repository.save(user);

		return Map.of(
				"message", "Tài khoản đã được đóng",
				"status", user.getStatus(),
				"deletedAt", user.getDeletedAt());
	}
}
