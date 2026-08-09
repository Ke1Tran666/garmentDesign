package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.UserAddressService;
import com.garmentDesign.service.UserStatusService;

import jakarta.transaction.Transactional;

import org.springframework.security.access.AccessDeniedException;

@Service
public class UserAddressServiceImpl implements UserAddressService {
	private final UserAddressRepository repository;
	private final UserRepository userRepository;
	private final UserStatusService userStatusService;

	public UserAddressServiceImpl(UserAddressRepository repository, UserRepository userRepository,
			UserStatusService userStatusService) {
		this.repository = repository;
		this.userRepository = userRepository;
		this.userStatusService = userStatusService;
	}

	private UserAddress requireOwnedAddress(String idUser, Long addressId) {
		return repository.findByAddressIdAndUser_IdUserAndDeletedAtIsNull(addressId, idUser).orElseThrow(
				() -> new AccessDeniedException("Không tìm thấy địa chỉ hoặc địa chỉ không thuộc người dùng hiện tại"));
	}

	@Override
	public List<UserAddress> findAll() {
		return repository.findAll();
	}

	@Override
	public List<UserAddress> findByUserId(String idUser) {
		return repository.findByUser_IdUserAndDeletedAtIsNull(idUser);
	}

	@Override
	public UserAddress findById(Long id) {
		return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
	}

	@Override
	public UserAddress save(UserAddress data) {
		return repository.save(data);
	}

	@Override
	@Transactional
	public UserAddress createByUser(String idUser, UserAddress data) {
		User user = userRepository.findById(idUser)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		UserAddress oldAddress = repository.findByUser_IdUserAndCompanyNameIgnoreCaseAndAddressIgnoreCase(idUser,
				data.getCompanyName(), data.getAddress()).orElse(null);

		UserAddress savedAddress;

		if (oldAddress != null) {
			oldAddress.setDeletedAt(null);
			oldAddress.setNote(data.getNote());
			oldAddress.setCompanyName(data.getCompanyName());
			oldAddress.setAddress(data.getAddress());

			savedAddress = repository.save(oldAddress);
		} else {
			data.setUser(user);
			data.setAddressId(null);
			data.setDeletedAt(null);

			savedAddress = repository.save(data);
		}

		userStatusService.refreshStatus(user);

		return savedAddress;
	}

	@Override
	public UserAddress update(Long id, UserAddress data) {
		UserAddress oldData = findById(id);

		oldData.setCompanyName(data.getCompanyName());
		oldData.setAddress(data.getAddress());
		oldData.setNote(data.getNote());

		return repository.save(oldData);
	}

	@Override
	public void delete(Long id) {
		UserAddress oldData = findById(id);

		oldData.setDeletedAt(java.time.LocalDateTime.now());

		repository.save(oldData);
	}

	@Override
	@Transactional
	public UserAddress setDefaultAddress(String idUser, Long addressId) {
		User user = userRepository.findById(idUser)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

		UserAddress address = requireOwnedAddress(idUser, addressId);

		user.setDefaultAddress(address);
		userRepository.save(user);

		return address;
	}

	@Override
	public UserAddress updateByUser(String idUser, Long addressId, UserAddress data) {
		UserAddress oldData = requireOwnedAddress(idUser, addressId);

		oldData.setCompanyName(data.getCompanyName());

		oldData.setAddress(data.getAddress());

		oldData.setNote(data.getNote());

		return repository.save(oldData);
	}

	@Override
	@Transactional
	public void deleteByUser(String idUser, Long addressId) {
		UserAddress address = requireOwnedAddress(idUser, addressId);

		User user = address.getUser();

		if (user.getDefaultAddress() != null && addressId.equals(user.getDefaultAddress().getAddressId())) {
			user.setDefaultAddress(null);
			userRepository.save(user);
		}

		address.setDeletedAt(java.time.LocalDateTime.now());

		repository.save(address);

		userStatusService.refreshStatus(user);
	}

}
