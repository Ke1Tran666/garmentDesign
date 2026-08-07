package com.garmentDesign.service.Impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.garmentDesign.dto.auth.AuthenticatedUser;
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

    private void validateUserStatus(User user) {
        if ("inactive".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đang tạm ngưng hoạt động. Vui lòng liên hệ hotline để được hỗ trợ.");
        }

        if ("banned".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hotline để được hỗ trợ.");
        }

        if ("delete".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị xóa. Nếu muốn khôi phục vui lòng liên hệ hotline để được hỗ trợ.");
        }
    }
    
    private AuthenticatedUser createLoginResult(User user) {
        if (user.getRole() == null) {
            throw new RuntimeException(
                "Tài khoản chưa được phân quyền"
            );
        }

        return new AuthenticatedUser(
            user.getIdUser(),
            user.getRole().getNameRole()
        );
    }

    private String removeVietnameseAccent(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{M}", "")
                .replace("Đ", "D")
                .replace("đ", "d");
    }

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

        return String.format("%-3s", lastName).replace(' ', 'O');
    }

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

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng để liên kết"));

        validateUserStatus(user);

        return user;
    }

    private void updateUserStatus(User user) {
        if ("delete".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        if ("banned".equalsIgnoreCase(user.getStatus())) {
            return;
        }

        boolean hasProfileInfo =
                user.getFullName() != null
                        && !user.getFullName().trim().isEmpty()
                        && user.getBirthday() != null
                        && user.getGender() != null
                        && !"Unknown".equalsIgnoreCase(user.getGender());

        boolean hasVerifiedContact =
                authProviderRepository
                        .findByUser_IdUserAndDeletedAtIsNull(user.getIdUser())
                        .stream()
                        .anyMatch(provider ->
                                provider.getEmailVerifiedAt() != null
                                        || provider.getPhoneVerifiedAt() != null
                        );

        if (hasProfileInfo && hasVerifiedContact) {
            user.setStatus("active");
        } else {
            user.setStatus("pending");
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public AuthenticatedUser login(String email,String password) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        if (password == null || password.isBlank()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }

        UserAuthProvider auth = authProviderRepository
                .findByEmailAndProvider(email.trim(),"local")
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        User user = auth.getUser();

        validateUserStatus(user);

        if (!auth.getPassword().equals(password)) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        return createLoginResult(user);
    }

    @Override
    public Map<String, Object> sendOtp(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        String normalizedPhone = phone.trim();

        UserAuthProvider existingProvider =
            authProviderRepository
                .findByPhoneAndProvider(normalizedPhone,"phone")
                .orElse(null);

        if (existingProvider != null) {
            validateUserStatus(existingProvider.getUser());
        }

        otpService.sendOtp(normalizedPhone,"phone");

        return Map.of("message","Đã gửi OTP");
    }

    @Override
    public AuthenticatedUser verifyPhoneOtp(String phone,String otp) {
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP không được để trống");
        }

        String normalizedPhone = phone.trim();

        otpService.verifyOtp(
            normalizedPhone,
            "phone",
            otp.trim()
        );

        UserAuthProvider provider = authProviderRepository
                .findByPhoneAndProvider(normalizedPhone,"phone")
                .orElse(null);

        User user;

        if (provider != null) {
            user = provider.getUser();

            validateUserStatus(user);

            provider.setDeletedAt(null);
            provider.setPhoneVerifiedAt(LocalDateTime.now());
            provider.setUpdatedAt(LocalDateTime.now());

            authProviderRepository.save(provider);
        } else {
            user = createPendingPhoneUser();

            UserAuthProvider newProvider = new UserAuthProvider();

            newProvider.setUser(user);
            newProvider.setProvider("phone");
            newProvider.setPhone(normalizedPhone);
            newProvider.setPhoneVerifiedAt(LocalDateTime.now());
            newProvider.setCreatedAt(LocalDateTime.now());
            newProvider.setUpdatedAt(LocalDateTime.now());
            newProvider.setDeletedAt(null);

            authProviderRepository.save(newProvider);
        }

        updateUserStatus(user);

        otpService.clearOtp(normalizedPhone,"phone");

        return createLoginResult(user);
    }

    @Override
    public Map<String, Object> sendEmailOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }

        authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản local với email này"));

        otpService.sendOtp(email, "email");

        return Map.of("message", "Đã gửi OTP xác thực email");
    }

    @Override
    public Map<String, Object> verifyEmailOtp(String email,String otp) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP không được để trống");
        }

        String normalizedEmail = email.trim().toLowerCase();

        UserAuthProvider provider = authProviderRepository
                .findByEmailAndProvider(normalizedEmail,"local")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản local"));

        otpService.verifyOtp(
            normalizedEmail,
            "email",
            otp.trim()
        );

        User user = provider.getUser();

        validateUserStatus(user);

        provider.setDeletedAt(null);
        provider.setEmailVerifiedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());

        authProviderRepository.save(provider);

        updateUserStatus(user);

        otpService.clearOtp(
            normalizedEmail,
            "email"
        );

        return Map.of("message","Xác thực email thành công");
    }

    @Override
    public Map<String, Object> register(
            String email,
            String password,
            String fullName,
            String gender,
            String birthday
    ) {
        UserAuthProvider existingAuth = authProviderRepository
                .findByEmailAndProvider(email, "local")
                .orElse(null);

        if (existingAuth != null) {
            User existingUser = existingAuth.getUser();

            if ("delete".equalsIgnoreCase(existingUser.getStatus())) {
                throw new RuntimeException("Email này thuộc tài khoản đã bị xóa. Nếu muốn khôi phục vui lòng liên hệ hotline để được hỗ trợ.");
            }

            throw new RuntimeException("Email đã tồn tại");
        }

        String idUser = generateRandom5Number();
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

        Role userRole = roleRepository.findById(3L)
                .orElseThrow(() -> new RuntimeException("Role user không tồn tại"));

        User user = new User();
        user.setIdUser(idUser);
        user.setUserCode(userCode);
        user.setFullName(fullName);
        user.setGender(gender);

        if (birthday != null && !birthday.isEmpty()) {
            user.setBirthday(LocalDate.parse(birthday));
        }

        user.setStatus("pending");
        user.setRole(userRole);

        user = userRepository.save(user);

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

        validateUserStatus(user);

        if ("pending".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn chưa hoàn tất đăng ký. Vui lòng liên hệ hotline để được hỗ trợ.");
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
    public AuthenticatedUser googleLogin(String accessToken) {
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
            validateUserStatus(user);
        } else {
            String idUser = generateRandom5Number();
            String prefixName = generateNameCode(fullName);
            String userCode = prefixName + "U00" + idUser;

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

        return createLoginResult(user);
    }
}