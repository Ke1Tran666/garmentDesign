package com.garmentDesign.service.Impl;

import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.service.AuthService;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthProviderRepository authProviderRepository;

    public AuthServiceImpl(UserAuthProviderRepository authProviderRepository) {
        this.authProviderRepository = authProviderRepository;
    }

    @Override
    public Map<String, Object> login(String email, String password) {
        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (!auth.getPassword().equals(password)) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        User user = auth.getUser();

        Map<String, Object> result = new HashMap<>();
        result.put("token", "fake-token-demo");
        result.put("user", user);

        return result;
    }
}