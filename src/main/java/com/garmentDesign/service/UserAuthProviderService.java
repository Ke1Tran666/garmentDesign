package com.garmentDesign.service;

import com.garmentDesign.entity.UserAuthProvider;
import java.util.List;

public interface UserAuthProviderService {
    List<UserAuthProvider> findAll();
    UserAuthProvider findById(Long id);
    UserAuthProvider save(UserAuthProvider data);
    UserAuthProvider update(Long id, UserAuthProvider data);
    void delete(Long id);
}
