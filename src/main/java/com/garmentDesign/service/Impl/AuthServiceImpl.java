package com.garmentDesign.service.Impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.garmentDesign.entity.Role;
import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.AuthService;

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
    
    //    REGISTER
    @Override
    public Map<String, Object> register(
            String email,
            String password,
            String fullName,
            String gender,
            String birthday
    ) {

        // Kiểm tra email đã tồn tại chưa
        boolean emailExists = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .isPresent();

        if (emailExists) {
            throw new RuntimeException("Email đã tồn tại");
        }

        String idUser = generateRandom5Number();

        /*
        HUNN03xxxxx
        */
        String prefixName;

        if (fullName.length() >= 3) {
            prefixName = fullName
                    .replaceAll("\\s+", "")
                    .substring(0, 3)
                    .toUpperCase();
        } else {
            prefixName = ("O" + fullName)
                    .substring(0, 3)
                    .toUpperCase();
        }

        String genderCode;

        switch (gender.toLowerCase()) {
            case "male":
                genderCode = "N";
                break;

            case "female":
                genderCode = "F";
                break;

            default:
                genderCode = "U";
                break;
        }

        String yearCode = "00";

        if (birthday != null && !birthday.isEmpty()) {
            yearCode = birthday.substring(2, 4);
        }

        String userCode = prefixName + genderCode + yearCode + idUser;

        // ROLE USER
        Role userRole = roleRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

        // USERS
        User user = new User();

        user.setIdUser(idUser);
        user.setUserCode(userCode);

        user.setFullName(fullName);
        user.setGender(gender);

        user.setBirthday(LocalDate.parse(birthday));

        user.setStatus("active");

        user.setRole(userRole);

        user = userRepository.save(user);

        // AUTH PROVIDER
        UserAuthProvider auth = new UserAuthProvider();

        auth.setUser(user);

        auth.setProvider("local");

        auth.setEmail(email);
        
        auth.setEmailVerifiedAt(null);

        auth.setPassword(password);

        authProviderRepository.save(auth);

        Map<String, Object> result = new HashMap<>();

        result.put("message", "Đăng ký thành công");

        return result;
    }
}