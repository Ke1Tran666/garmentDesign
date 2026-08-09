package com.garmentDesign.service.Impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository repository;
	private final UserAuthProviderRepository authProviderRepository;
	private final UserAddressRepository addressRepository;
	private final PasswordService passwordService;
	private final UserStatusService userStatusService;

	public UserServiceImpl(UserRepository repository, UserAuthProviderRepository authProviderRepository,
			UserAddressRepository addressRepository, PasswordService passwordService,
			UserStatusService userStatusService) {
		this.repository = repository;
		this.authProviderRepository = authProviderRepository;
		this.addressRepository = addressRepository;
		this.passwordService = passwordService;
		this.userStatusService = userStatusService;
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
	public User updateProfile(String id, UpdateProfileRequest request) {
		User user = findById(id);

		user.setFullName(request.getFullName());
		user.setGender(request.getGender());
		user.setBirthday(request.getBirthday());

		String newUserCode = generateUserCode(user.getFullName(), user.getGender(), user.getBirthday(),
				user.getUserCode(), user.getIdUser());

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
	public Map<String, Object> deleteAvatar(String idUser) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		user.setAvatar(null);

		user.setUpdatedAt(LocalDateTime.now());

		repository.save(user);

		Map<String, Object> result = new HashMap<>();

		result.put("message", "Xóa avatar thành công");
		result.put("avatar", null);
		result.put("user", user);

		return result;
	}

	// Upload Avatar
	@Override
	public Map<String, Object> uploadAvatar(String idUser, MultipartFile file) {

		User user = repository.findById(idUser).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		try {

			if (file == null || file.isEmpty()) {
				throw new RuntimeException("Vui lòng chọn ảnh");
			}

			String uploadDir = "uploads/avatars/";

			File folder = new File(uploadDir);

			if (!folder.exists()) {
				folder.mkdirs();
			}

			String originalName = file.getOriginalFilename();

			String extension = "";

			if (originalName != null && originalName.contains(".")) {
				extension = originalName.substring(originalName.lastIndexOf("."));
			}

			String fileName = idUser + "_" + System.currentTimeMillis() + extension;

			Path filePath = Paths.get(uploadDir + fileName);

			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			String avatarUrl = "http://localhost:8080/uploads/avatars/" + fileName;

			user.setAvatar(avatarUrl);

			repository.save(user);

			Map<String, Object> result = new HashMap<>();

			result.put("message", "Upload avatar thành công");
			result.put("avatar", avatarUrl);
			result.put("user", user);

			return result;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	// Change Password
	@Override
	@Transactional
	public Map<String, Object> changePassword(String idUser, String oldPassword, String newPassword) {

		if (idUser == null || idUser.isBlank()) {
			throw new RuntimeException("Không tìm thấy người dùng");
		}

		if (oldPassword == null || oldPassword.isBlank()) {
			throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại");
		}

		passwordService.validateNewPassword(newPassword);

		if (oldPassword.equals(newPassword)) {
			throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
		}

		UserAuthProvider localProvider = authProviderRepository
				.findByUser_IdUserAndProviderAndDeletedAtIsNull(idUser, "local")
				.orElseThrow(() -> new RuntimeException("Tài khoản của bạn chưa liên kết đăng nhập bằng mật khẩu."));

		String storedPassword = localProvider.getPassword();

		if (storedPassword == null || storedPassword.isBlank()) {
			throw new RuntimeException("Tài khoản local chưa có mật khẩu.");
		}

		/*
		 * PasswordService kiểm tra được cả BCrypt và plaintext cũ.
		 */
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
