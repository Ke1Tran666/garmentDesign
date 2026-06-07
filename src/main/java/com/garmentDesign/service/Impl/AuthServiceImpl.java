package com.garmentDesign.service.Impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.garmentDesign.entity.Role;
import com.garmentDesign.entity.User;
import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.AuthService;
import com.garmentDesign.service.OtpService;

@Service
public class AuthServiceImpl implements AuthService {


	private final UserAuthProviderRepository authProviderRepository;
    
    private final UserRepository userRepository;
    
    private final RoleRepository roleRepository;
    
    private final OtpService otpService;

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    public AuthServiceImpl(
            UserAuthProviderRepository authProviderRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OtpService otpService
    ) {
        this.authProviderRepository = authProviderRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.otpService = otpService;
    }
    
    //    Hàm bỏ dấu tiếng Việt
    private String removeVietnameseAccent(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{M}", "")
                .replace("Đ", "D")
                .replace("đ", "d");
    }
    
    // Hàm tạo mã tên
    private String generateNameCode(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return "USE";
        }

        String cleanName = removeVietnameseAccent(fullName)
                .trim()
                .replaceAll("\\s+", " ");

        String[] words = cleanName.split(" ");

        String lastName = words[words.length - 1]
                .replaceAll("[^a-zA-Z]", "")
                .toUpperCase();

        if (lastName.length() >= 3) {
            return lastName.substring(0, 3);
        }

        return String.format("%-3s", lastName)
                .replace(' ', 'O');
    }
    
    //    Hàm random
    private String generateRandom5Number() {
    	String id;
        do {
            id = String.format("%05d", new Random().nextInt(100000));
        } while (userRepository.existsById(id));

        return id;
    }
    
    private User createPendingPhoneUser() {
        String idUser = generateRandom5Number();
        String userCode = "USEU00" + idUser;

        Role userRole = roleRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

        User user = new User();
        user.setIdUser(idUser);
        user.setUserCode(userCode);
        user.setGender("Unknown");
        user.setStatus("pending");
        user.setRole(userRole);

        return userRepository.save(user);
    }
    
    private User getUserForLinking(String idUser) {
        if (idUser == null || idUser.trim().isEmpty()) {
            return createPendingPhoneUser();
        }

        return userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng để liên kết"));
    }
    
    //    EMAIL
    @Override
    public Map<String, Object> login(String email, String password) {
        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        User user = auth.getUser();

        if ("inactive".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đang tạm ngưng hoạt động. Vui lòng liên hệ hotline để được hỗ trợ.");
        }

        if ("banned".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hotline để được hỗ trợ.");
        }

        if (!auth.getPassword().equals(password)) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        Map<String, Object> result = new HashMap<>();
        
        // login
        result.put("token", "fake-token-demo");
        result.put("idUser", user.getIdUser());

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
        
        // loginPhone
        result.put("token", "fake-token-demo");
        result.put("idUser", user.getIdUser());

        return result;
    }
    
    //    OTP Số ĐT
    @Override
    public Map<String, Object> sendOtp(String phone) {

        otpService.sendOtp(phone, "phone");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Đã gửi OTP");

        return result;
    }

    @Override
    public Map<String, Object> verifyOtp(String idUser, String phone, String otp, String mode) {

        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        otpService.verifyOtp(phone, "phone", otp);

        String normalizedMode = mode == null || mode.trim().isEmpty()
                ? "existing"
                : mode.trim();

        UserAuthProvider auth = authProviderRepository
                .findByPhoneAndProvider(phone, "phone")
                .orElse(null);

        User user;

        if (auth != null) {
            user = auth.getUser();

            if ("new".equalsIgnoreCase(normalizedMode)
                    && idUser != null
                    && !idUser.trim().isEmpty()
                    && !user.getIdUser().equals(idUser)) {
                throw new RuntimeException("Số điện thoại này đã được liên kết với tài khoản khác");
            }

            auth.setDeletedAt(null);
            auth.setPhoneVerifiedAt(LocalDateTime.now());
            auth.setUpdatedAt(LocalDateTime.now());

            authProviderRepository.save(auth);
        } else {
            user = getUserForLinking(idUser);

            UserAuthProvider newAuth = new UserAuthProvider();
            newAuth.setUser(user);
            newAuth.setProvider("phone");
            newAuth.setPhone(phone);
            newAuth.setPhoneVerifiedAt(LocalDateTime.now());
            newAuth.setCreatedAt(LocalDateTime.now());
            newAuth.setUpdatedAt(LocalDateTime.now());
            newAuth.setDeletedAt(null);

            authProviderRepository.save(newAuth);
        }

        otpService.clearOtp(phone, "phone");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Xác thực số điện thoại thành công");
        result.put("token", "fake-token-demo");
        result.put("idUser", user.getIdUser());

        return result;
    }
    
    //    OTP Email Local
    @Override
    public Map<String, Object> sendEmailOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }

        authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản local với email này"));

        otpService.sendOtp(email, "email");

        return Map.of(
                "message", "Đã gửi OTP xác thực email"
        );
    }
    
    @Override
    public Map<String, Object> verifyEmailOtp(String idUser, String email, String otp, String mode) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }

        boolean valid = otpService.verifyOtp(email, "email", otp);

        if (!valid) {
            throw new RuntimeException("OTP không đúng hoặc đã hết hạn");
        }

        String normalizedMode = mode == null || mode.trim().isEmpty()
                ? "existing"
                : mode.trim();

        UserAuthProvider provider = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElse(null);

        User user;

        if (provider != null) {
            user = provider.getUser();

            if ("new".equalsIgnoreCase(normalizedMode)
                    && idUser != null
                    && !idUser.trim().isEmpty()
                    && !user.getIdUser().equals(idUser)) {
                throw new RuntimeException("Email này đã được liên kết với tài khoản khác");
            }

            provider.setDeletedAt(null);
            provider.setEmailVerifiedAt(LocalDateTime.now());
            provider.setUpdatedAt(LocalDateTime.now());

            authProviderRepository.save(provider);
        } else {
            user = getUserForLinking(idUser);

            UserAuthProvider newProvider = new UserAuthProvider();
            newProvider.setUser(user);
            newProvider.setProvider("local");
            newProvider.setEmail(email);
            newProvider.setEmailVerifiedAt(LocalDateTime.now());
            newProvider.setCreatedAt(LocalDateTime.now());
            newProvider.setUpdatedAt(LocalDateTime.now());
            newProvider.setDeletedAt(null);

            authProviderRepository.save(newProvider);
        }

        otpService.clearOtp(email, "email");

        return Map.of(
                "message", "Xác thực email thành công",
                "idUser", user.getIdUser()
        );
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
        String prefixName = generateNameCode(fullName);

        String genderCode;

        switch (gender.toLowerCase()) {
            case "male":
                genderCode = "M";
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

        user.setStatus("pending");

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
    
    @Override
    public Map<String, Object> forgotPassword(String email) {

        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        User user = auth.getUser();

        switch (user.getStatus().toLowerCase()) {

            case "active":
                break;

            case "pending":
                throw new RuntimeException(
                    "Tài khoản của bạn chưa hoàn tất đăng ký. Vui lòng liên hệ hotline để được hỗ trợ."
                );

            case "inactive":
                throw new RuntimeException(
                    "Tài khoản của bạn hiện đang tạm ngưng hoạt động. Vui lòng liên hệ hotline để được hỗ trợ."
                );

            case "banned":
                throw new RuntimeException(
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hotline để được hỗ trợ."
                );

            default:
                throw new RuntimeException(
                    "Trạng thái tài khoản không hợp lệ."
                );
        }

        otpService.sendOtp(email, "email");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Đã gửi OTP về email");

        return result;
    }

    @Override
    public Map<String, Object> verifyForgotOtp(String email, String otp) {
        authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        otpService.verifyOtp(email, "email", otp);
        otpService.markVerified(email, "email");
        otpService.clearOtp(email, "email");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Xác thực OTP thành công");

        return result;
    }

    @Override
    public Map<String, Object> resetPassword(String email, String newPassword) {
        if (!otpService.isVerified(email, "email")) {
            throw new RuntimeException("Bạn chưa xác thực OTP hoặc phiên đã hết hạn");
        }

        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        auth.setPassword(newPassword);
        authProviderRepository.save(auth);

        otpService.clearVerified(email, "email");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Đổi mật khẩu thành công");

        return result;
    }
    
    @Override
    public Map<String, Object> googleLogin(String accessToken) {

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("Google access token không hợp lệ");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> googleUser = response.getBody();

        if (googleUser == null || googleUser.get("email") == null) {
            throw new RuntimeException("Không lấy được thông tin Google");
        }

        String googleId = googleUser.get("sub").toString();
        String email = googleUser.get("email").toString();
        String fullName = googleUser.get("name") != null
                ? googleUser.get("name").toString()
                : "Google User";

        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email, "google")
                .orElse(null);

        User user;

        if (auth != null) {
            user = auth.getUser();

            if ("inactive".equalsIgnoreCase(user.getStatus())) {
                throw new RuntimeException("Tài khoản của bạn đang tạm ngưng hoạt động. Vui lòng liên hệ hotline để được hỗ trợ.");
            }

            if ("banned".equalsIgnoreCase(user.getStatus())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hotline để được hỗ trợ.");
            }

        } else {
            String idUser = generateRandom5Number();

            String prefixName = generateNameCode(fullName);
            String genderCode = "U";
            String yearCode = "00";
            String userCode = prefixName + genderCode + yearCode + idUser;

            Role userRole = roleRepository.findById(3L)
                    .orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

            user = new User();

            user.setIdUser(idUser);
            user.setUserCode(userCode);
            user.setFullName(fullName);
            user.setGender("Unknown");
            user.setStatus("active");
            user.setRole(userRole);

            user = userRepository.save(user);

            UserAuthProvider newAuth = new UserAuthProvider();

            newAuth.setUser(user);
            newAuth.setProvider("google");
            newAuth.setProviderId(googleId);
            newAuth.setEmail(email);
            newAuth.setEmailVerifiedAt(LocalDateTime.now());

            authProviderRepository.save(newAuth);
        }

        Map<String, Object> result = new HashMap<>();
        
        // googleLogin
        result.put("token", "fake-token-demo");
        result.put("idUser", user.getIdUser());

        return result;
    }
}