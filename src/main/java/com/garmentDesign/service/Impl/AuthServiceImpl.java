package com.garmentDesign.service.Impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.AuthService;
import com.garmentDesign.entity.Role;
import com.garmentDesign.repository.RoleRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthProviderRepository authProviderRepository;
    
    private final Map<String, String> otpStorage = new HashMap<>();
    
    private final UserRepository userRepository;
    
    private final RoleRepository roleRepository;

    public AuthServiceImpl(
            UserAuthProviderRepository authProviderRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.authProviderRepository = authProviderRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }
    
    //    Hàm random
    private String generateRandom5Number() {
        return String.format("%05d", new Random().nextInt(100000));
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
                .orElse(null);

        User user;

        if (auth != null) {
            user = auth.getUser();
        } else {
            user = new User();

            String idUser = generateRandom5Number();

            /*
            USE = user chưa có tên
            N   = Unknown
            00  = chưa có năm sinh
            + idUser
            */
            String userCode = "USEU00" + idUser;

            user.setIdUser(idUser);
            user.setUserCode(userCode);
            
            user.setGender("Unknown");

            user.setStatus("pending");
            
            Role userRole = roleRepository.findById(3L)
                    .orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

            user.setRole(userRole);
            
            user = userRepository.save(user);

            UserAuthProvider newAuth = new UserAuthProvider();
            newAuth.setUser(user);
            newAuth.setProvider("phone");
            newAuth.setPhone(phone);
            newAuth.setPhoneVerifiedAt(LocalDateTime.now());

            authProviderRepository.save(newAuth);
        }

        otpStorage.remove(phone);

        Map<String, Object> result = new HashMap<>();
        result.put("token", "fake-token-demo");
        result.put("user", user);

        return result;
    }
}