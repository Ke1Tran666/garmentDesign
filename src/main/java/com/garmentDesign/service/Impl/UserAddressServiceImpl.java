package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.UserAddressService;

@Service
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressRepository repository;
    private final UserRepository userRepository;

    public UserAddressServiceImpl(
            UserAddressRepository repository,
            UserRepository userRepository
        ) {
            this.repository = repository;
            this.userRepository = userRepository;
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
    public UserAddress createByUser(String idUser, UserAddress data) {
        User user = userRepository.findById(idUser)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + idUser));

        UserAddress oldAddress = repository
            .findByUser_IdUserAndCompanyNameIgnoreCaseAndAddressIgnoreCase(
                idUser,
                data.getCompanyName(),
                data.getAddress()
            )
            .orElse(null);

        if (oldAddress != null) {
            oldAddress.setDeletedAt(null);
            oldAddress.setNote(data.getNote());
            oldAddress.setCompanyName(data.getCompanyName());
            oldAddress.setAddress(data.getAddress());

            return repository.save(oldAddress);
        }

        data.setUser(user);
        data.setAddressId(null);
        data.setDeletedAt(null);

        return repository.save(data);
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
    public UserAddress setDefaultAddress(String idUser, Long addressId) {
        User user = userRepository.findById(idUser)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + idUser));

        UserAddress address = repository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ với id: " + addressId));

        if (!address.getUser().getIdUser().equals(idUser)) {
            throw new RuntimeException("Địa chỉ này không thuộc user hiện tại");
        }

        user.setDefaultAddress(address);
        userRepository.save(user);

        return address;
    }

}
