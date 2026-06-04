package com.garmentDesign.service.Impl;

import java.util.List;
import java.util.HashMap;
import java.util.Map;



import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.User;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.UserService;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserAddressRepository;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    private final UserAuthProviderRepository authProviderRepository;

    private final UserAddressRepository addressRepository;

    public UserServiceImpl(
            UserRepository repository,
            UserAuthProviderRepository authProviderRepository,
            UserAddressRepository addressRepository
    ) {
        this.repository = repository;
        this.authProviderRepository = authProviderRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    public List<User> findAll() {
        return repository.findAll();
    }
    
    @Override
    public Map<String, Object> getProfile(String idUser) {

        User user = repository.findById(idUser)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng"));

        Map<String, Object> profile = new HashMap<>();

        profile.put("user", user);

        profile.put(
                "authProviders",
                authProviderRepository
                        .findByUser_IdUserAndDeletedAtIsNull(idUser)
        );

        profile.put(
                "addresses",
                addressRepository
                        .findByUser_IdUserAndDeletedAtIsNull(idUser)
        );

        profile.put(
                "defaultAddress",
                user.getDefaultAddress()
        );

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
    public User update(String id, User data) {
        User oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}
