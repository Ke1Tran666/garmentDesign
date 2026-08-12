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

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository repository;
	private final UserAuthProviderRepository authProviderRepository;
	private final UserAddressRepository addressRepository;
	private final PasswordService passwordService;
	private final UserStatusService userStatusService;
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(UserServiceImpl.class);

	private static final long MAX_AVATAR_SIZE =
			5L * 1024L * 1024L;

	private static final int MAX_AVATAR_DIMENSION = 4096;

	private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES =
			Set.of(
					"image/jpeg",
					"image/png"
			);

	private final Path avatarDirectory;
	private final String publicBaseUrl;

	public UserServiceImpl(
			UserRepository repository,
			UserAuthProviderRepository authProviderRepository,
			UserAddressRepository addressRepository,
			PasswordService passwordService,
			UserStatusService userStatusService,
			@Value("${app.upload.avatar-dir:uploads/avatars}")
			String avatarDirectory,
			@Value("${app.public-base-url:http://localhost:8082}")
			String publicBaseUrl) {

		this.repository = repository;
		this.authProviderRepository = authProviderRepository;
		this.addressRepository = addressRepository;
		this.passwordService = passwordService;
		this.userStatusService = userStatusService;

		this.avatarDirectory = Path.of(avatarDirectory)
				.toAbsolutePath()
				.normalize();

		if (publicBaseUrl == null
				|| publicBaseUrl.isBlank()) {

			throw new IllegalStateException(
					"APP_PUBLIC_BASE_URL chưa được cấu hình"
			);
		}

		this.publicBaseUrl =
				publicBaseUrl.trim().replaceAll("/+$", "");
	}
	
	private String validateAvatarAndGetExtension(
			MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw new RuntimeException(
					"Vui lòng chọn ảnh đại diện"
			);
		}

		if (file.getSize() > MAX_AVATAR_SIZE) {
			throw new RuntimeException(
					"Ảnh đại diện không được vượt quá 5 MB"
			);
		}

		String contentType = file.getContentType();

		if (contentType == null
				|| !ALLOWED_AVATAR_CONTENT_TYPES.contains(
						contentType.toLowerCase(Locale.ROOT)
				)) {

			throw new RuntimeException(
					"Chỉ chấp nhận ảnh JPEG hoặc PNG"
			);
		}

		try (
				InputStream inputStream = file.getInputStream();
				ImageInputStream imageInputStream =
						ImageIO.createImageInputStream(inputStream)
		) {
			if (imageInputStream == null) {
				throw new RuntimeException(
						"File ảnh không hợp lệ"
				);
			}

			Iterator<ImageReader> readers =
					ImageIO.getImageReaders(imageInputStream);

			if (!readers.hasNext()) {
				throw new RuntimeException(
						"File tải lên không phải ảnh hợp lệ"
				);
			}

			ImageReader reader = readers.next();

			try {
				reader.setInput(
						imageInputStream,
						true,
						true
				);

				int width = reader.getWidth(0);
				int height = reader.getHeight(0);

				if (width <= 0 || height <= 0) {
					throw new RuntimeException(
							"Kích thước ảnh không hợp lệ"
					);
				}

				if (width > MAX_AVATAR_DIMENSION
						|| height > MAX_AVATAR_DIMENSION) {

					throw new RuntimeException(
							"Chiều rộng và chiều cao ảnh " +
							"không được vượt quá 4096 pixel"
					);
				}

				String format =
						reader.getFormatName()
								.toLowerCase(Locale.ROOT);

				return switch (format) {
					case "jpg", "jpeg" -> ".jpg";
					case "png" -> ".png";
					default -> throw new RuntimeException(
							"Chỉ chấp nhận ảnh JPEG hoặc PNG"
					);
				};
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			throw new RuntimeException(
					"Không thể đọc file ảnh",
					exception
			);
		}
	}

	private void deleteStoredAvatarFile(
			String avatarUrl) {

		if (avatarUrl == null || avatarUrl.isBlank()) {
			return;
		}

		String normalizedUrl =
				avatarUrl.replace('\\', '/');

		String marker = "/uploads/avatars/";

		int markerIndex =
				normalizedUrl.indexOf(marker);

		if (markerIndex < 0) {
			/*
			 * Không xóa URL ngoài hệ thống.
			 */
			return;
		}

		String fileName = normalizedUrl.substring(
				markerIndex + marker.length()
		);

		if (fileName.isBlank()
				|| fileName.contains("/")
				|| fileName.contains("\\")) {

			return;
		}

		Path filePath = avatarDirectory
				.resolve(fileName)
				.normalize();

		if (!filePath.startsWith(avatarDirectory)) {
			return;
		}

		try {
			Files.deleteIfExists(filePath);
		} catch (IOException exception) {
			LOGGER.warn(
					"Không thể xóa avatar cũ: {}",
					filePath,
					exception
			);
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
		if (oldUserCode != null && oldUserCode.length() >= 5) {
			return oldUserCode.substring(oldUserCode.length() - 5);
		}

		if (idUser != null && idUser.length() >= 5) {
			return idUser.substring(idUser.length() - 5);
		}

		return "00000";
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
	public User updateProfile(
			String id,
			UpdateProfileRequest request) {

		if (request == null) {
			throw new RuntimeException(
					"Thông tin cập nhật không hợp lệ"
			);
		}

		User user = findById(id);

		String fullName = request.getFullName() == null
				? ""
				: request.getFullName().trim();

		String gender = request.getGender() == null
				? ""
				: request.getGender().trim();

		LocalDate birthday = request.getBirthday();

		if (fullName.isBlank()) {
			throw new RuntimeException(
					"Vui lòng nhập họ và tên"
			);
		}

		if (birthday == null) {
			throw new RuntimeException(
					"Vui lòng nhập ngày sinh"
			);
		}

		if (!birthday.isBefore(LocalDate.now())) {
			throw new RuntimeException(
					"Ngày sinh phải nhỏ hơn ngày hiện tại"
			);
		}

		boolean validGender =
				"Male".equalsIgnoreCase(gender)
				|| "Female".equalsIgnoreCase(gender)
				|| "Unknown".equalsIgnoreCase(gender);

		if (!validGender) {
			throw new RuntimeException(
					"Giới tính không hợp lệ"
			);
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

		String newUserCode = generateUserCode(
				user.getFullName(),
				user.getGender(),
				user.getBirthday(),
				user.getUserCode(),
				user.getIdUser()
		);

		user.setUserCode(newUserCode);
		user.setUpdatedAt(LocalDateTime.now());

		repository.save(user);

		return userStatusService.refreshStatus(user);
	}

	@Override
	public void delete(String id) {
		deleteAccount(id);
	}

	// Delete Avatar
	@Override
	@Transactional
	public Map<String, Object> deleteAvatar(
			String idUser) {

		User user = repository.findById(idUser)
				.orElseThrow(() ->
						new RuntimeException(
								"Không tìm thấy người dùng"
						)
				);

		String oldAvatar = user.getAvatar();

		user.setAvatar(null);
		user.setUpdatedAt(LocalDateTime.now());

		repository.saveAndFlush(user);

		deleteStoredAvatarFile(oldAvatar);

		Map<String, Object> result = new HashMap<>();

		result.put(
				"message",
				"Xóa avatar thành công"
		);
		result.put("avatar", null);

		return result;
	}

	// Upload Avatar
	@Override
	@Transactional
	public Map<String, Object> uploadAvatar(
			String idUser,
			MultipartFile file) {

		User user = repository.findById(idUser)
				.orElseThrow(() ->
						new RuntimeException(
								"Không tìm thấy người dùng"
						)
				);

		String extension =
				validateAvatarAndGetExtension(file);

		try {
			Files.createDirectories(avatarDirectory);
		} catch (IOException exception) {
			throw new RuntimeException(
					"Không thể tạo thư mục lưu avatar",
					exception
			);
		}

		String fileName =
				idUser
				+ "_"
				+ UUID.randomUUID()
				+ extension;

		Path filePath = avatarDirectory
				.resolve(fileName)
				.normalize();

		if (!filePath.startsWith(avatarDirectory)) {
			throw new RuntimeException(
					"Đường dẫn lưu avatar không hợp lệ"
			);
		}

		try (InputStream inputStream =
				file.getInputStream()) {

			Files.copy(inputStream, filePath);
		} catch (IOException exception) {
			throw new RuntimeException(
					"Không thể lưu ảnh đại diện",
					exception
			);
		}

		String oldAvatar = user.getAvatar();

		String avatarUrl =
				publicBaseUrl
				+ "/uploads/avatars/"
				+ fileName;

		try {
			user.setAvatar(avatarUrl);
			user.setUpdatedAt(LocalDateTime.now());

			repository.saveAndFlush(user);
		} catch (RuntimeException exception) {
			try {
				Files.deleteIfExists(filePath);
			} catch (IOException cleanupException) {
				LOGGER.warn(
						"Không thể dọn avatar sau khi lưu DB thất bại",
						cleanupException
				);
			}

			throw exception;
		}

	deleteStoredAvatarFile(oldAvatar);

		return Map.of(
				"message",
				"Upload avatar thành công",
				"avatar",
				avatarUrl
		);
	}

	// Change Password
	@Override
	@Transactional
	public Map<String, Object> changePassword(String idUser, String oldPassword, String newPassword) {
		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Không tìm thấy người dùng");
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

		authProviderRepository.save(localProvider);

		return Map.of("message", "Đổi mật khẩu thành công");
	}

	@Override
	public Map<String, Object> exportUserData(String idUser) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		List<UserAuthProvider> authProviders = authProviderRepository.findByUser_IdUserAndDeletedAtIsNull(idUser);

		List<UserAddress> addresses = addressRepository.findByUser_IdUserAndDeletedAtIsNull(idUser);

		// Không export password
		authProviders.forEach(provider -> provider.setPassword(null));

		Map<String, Object> result = new HashMap<>();

		result.put("user", user);
		result.put("authProviders", authProviders);
		result.put("addresses", addresses);
		result.put("defaultAddress", user.getDefaultAddress());

		return result;
	}

	@Override
	public Map<String, Object> deleteAccount(String idUser) {
		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		LocalDateTime now = LocalDateTime.now();

		// Soft delete user
		user.setDeletedAt(now);
		user.setUpdatedAt(now);
		user.setStatus("delete");
		user.setDefaultAddress(null);

		// Soft delete auth providers
		List<UserAuthProvider> authProviders = authProviderRepository.findByUser_IdUserAndDeletedAtIsNull(idUser);

		authProviders.forEach(provider -> {
			provider.setDeletedAt(now);
			provider.setUpdatedAt(now);
		});

		// Soft delete addresses
		List<UserAddress> addresses = addressRepository.findByUser_IdUserAndDeletedAtIsNull(idUser);

		addresses.forEach(address -> {
			address.setDeletedAt(now);
			address.setUpdatedAt(now);
		});

		authProviderRepository.saveAll(authProviders);
		addressRepository.saveAll(addresses);
		repository.save(user);

		return Map.of("message", "Tài khoản đã được xóa", "status", user.getStatus(), "deletedAt", user.getDeletedAt());
	}
}
