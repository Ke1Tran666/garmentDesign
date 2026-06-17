package com.garmentDesign.service;

import com.garmentDesign.entity.UserAddress;
import java.util.List;

public interface UserAddressService {
    List<UserAddress> findAll();
    List<UserAddress> findByUserId(String idUser);
    UserAddress findById(Long id);
    UserAddress save(UserAddress data);
    UserAddress update(Long id, UserAddress data);
    UserAddress setDefaultAddress(String idUser, Long addressId);
    void delete(Long id);
}
