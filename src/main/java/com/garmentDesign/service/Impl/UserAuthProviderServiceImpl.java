package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.service.UserAuthProviderService;

@Service
public class UserAuthProviderServiceImpl implements UserAuthProviderService {
    private final UserAuthProviderRepository repository;

    public UserAuthProviderServiceImpl(UserAuthProviderRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserAuthProvider> findAll() {
        return repository.findAll();
    }

    @Override
    public UserAuthProvider findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public UserAuthProvider save(UserAuthProvider data) {
        return repository.save(data);
    }

    @Override
    public UserAuthProvider update(Long id, UserAuthProvider data) {
        UserAuthProvider oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(Long id) {
        UserAuthProvider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết với id: " + id));

        String idUser = provider.getUser().getIdUser();

        long totalProviders = repository.countByUser_IdUserAndDeletedAtIsNull(idUser);

        if (totalProviders <= 1) {
            throw new RuntimeException("Tài khoản phải có ít nhất 1 phương thức đăng nhập.");
        }

        provider.setDeletedAt(java.time.LocalDateTime.now());
        provider.setUpdatedAt(java.time.LocalDateTime.now());

        repository.save(provider);
    }
}
