package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.service.UserAddressService;

@Service
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressRepository repository;

    public UserAddressServiceImpl(UserAddressRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserAddress> findAll() {
        return repository.findAll();
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
    public UserAddress update(Long id, UserAddress data) {
        UserAddress oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
