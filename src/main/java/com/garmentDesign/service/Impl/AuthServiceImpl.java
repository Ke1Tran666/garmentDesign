package com.garmentDesign.service.Impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthProviderRepository authProviderRepository;
    
    private final Map<String, String> otpStorage = new HashMap<>();

    public AuthServiceImpl(UserAuthProviderRepository authProviderRepository) {
        this.authProviderRepository = authProviderRepository;
    }
    
    //    EMAIL
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
    
    //    PHONE
    @Override
    public Map<String, Object> loginPhone(String phone) {

        UserAuthProvider auth = authProviderRepository
                .findByPhoneAndProvider(phone, "phone")
                .orElseThrow(() -> new RuntimeException("Số điện thoại không tồn tại"));

        User user = auth.getUser();

        Map<String, Object> result = new HashMap<>();
        result.put("token", "fake-token-demo");
        result.put("user", user);

        return result;
    }
    
    //    OTP
    @Override
    public Map<String, Object> sendOtp(String phone) {
        authProviderRepository
            .findByPhoneAndProvider(phone, "phone")
            .orElseThrow(() -> new RuntimeException("Số điện thoại không tồn tại"));

        String otp = String.format("%06d", new Random().nextInt(1000000));

        otpStorage.put(phone, otp);

        System.out.println("OTP của " + phone + ": " + otp);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Đã gửi OTP");

        return result;
    }

    @Override
    public Map<String, Object> verifyOtp(String phone, String otp) {
        String savedOtp = otpStorage.get(phone);

        if (savedOtp == null || !savedOtp.equals(otp)) {
            throw new RuntimeException("OTP không chính xác");
        }

        UserAuthProvider auth = authProviderRepository
            .findByPhoneAndProvider(phone, "phone")
            .orElseThrow(() -> new RuntimeException("Số điện thoại không tồn tại"));

        otpStorage.remove(phone);

        Map<String, Object> result = new HashMap<>();
        result.put("token", "fake-token-demo");
        result.put("user", auth.getUser());

        return result;
    }
}