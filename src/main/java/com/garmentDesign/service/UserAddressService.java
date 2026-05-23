package com.garmentDesign.service;

import com.garmentDesign.entity.UserAddress;
import java.util.List;

public interface UserAddressService {
    List<UserAddress> findAll();
    UserAddress findById(Long id);
    UserAddress save(UserAddress data);
    UserAddress update(Long id, UserAddress data);
    void delete(Long id);
}
