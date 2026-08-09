package com.garmentDesign.service.Impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentDesign.entity.User;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.UserStatusService;

@Service
public class UserStatusServiceImpl implements UserStatusService {

	private final UserRepository userRepository;

	private final UserAuthProviderRepository authProviderRepository;

	private final UserAddressRepository addressRepository;

	public UserStatusServiceImpl(UserRepository userRepository, UserAuthProviderRepository authProviderRepository,
			UserAddressRepository addressRepository) {
		this.userRepository = userRepository;
		this.authProviderRepository = authProviderRepository;
		this.addressRepository = addressRepository;
	}

	@Override
	@Transactional
	public User refreshStatus(String idUser) {
		User user = userRepository.findById(idUser)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		return refreshStatus(user);
	}

	@Override
	@Transactional
	public User refreshStatus(User user) {
		if (user == null) {
			throw new RuntimeException("Không tìm thấy người dùng");
		}

		String currentStatus = user.getStatus();

		/*
		 * Không tự động mở lại tài khoản do quản trị viên khóa/ngừng hoặc đã xóa.
		 */
		if ("inactive".equalsIgnoreCase(currentStatus) || "banned".equalsIgnoreCase(currentStatus)
				|| "delete".equalsIgnoreCase(currentStatus)) {
			return user;
		}

		boolean hasFullName = user.getFullName() != null && !user.getFullName().trim().isEmpty();

		boolean hasBirthday = user.getBirthday() != null;

		boolean hasGender = user.getGender() != null && !user.getGender().isBlank()
				&& !"Unknown".equalsIgnoreCase(user.getGender());

		boolean hasProfileInfo = hasFullName && hasBirthday && hasGender;

		boolean hasVerifiedContact = authProviderRepository.findByUser_IdUserAndDeletedAtIsNull(user.getIdUser())
				.stream()
				.anyMatch(provider -> provider.getEmailVerifiedAt() != null || provider.getPhoneVerifiedAt() != null);

		boolean hasAddress = addressRepository.existsByUser_IdUserAndDeletedAtIsNull(user.getIdUser());

		if (hasProfileInfo && hasVerifiedContact && hasAddress) {
			user.setStatus("active");
		} else {
			user.setStatus("pending");
		}

		user.setUpdatedAt(LocalDateTime.now());

		return userRepository.save(user);
	}
}