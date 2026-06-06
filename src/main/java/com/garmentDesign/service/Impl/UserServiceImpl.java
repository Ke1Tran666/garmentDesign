package com.garmentDesign.service.Impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.garmentDesign.dto.user.UpdateProfileRequest;
import com.garmentDesign.entity.User;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.repository.UserAuthProviderRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    private final UserAuthProviderRepository authProviderRepository;

    private final UserAddressRepository addressRepository;

    public UserServiceImpl(
            UserRepository repository,
            UserAuthProviderRepository authProviderRepository,
            UserAddressRepository addressRepository
    ) {
        this.repository = repository;
        this.authProviderRepository = authProviderRepository;
        this.addressRepository = addressRepository;
    }
    
    private String generateUserCode(
            String fullName,
            String gender,
            LocalDate birthday,
            String oldUserCode,
            String idUser
    ) {
        String nameCode = getNameCode(fullName);
        String genderCode = getGenderCode(gender);
        String yearCode = getYearCode(birthday);
        String suffixCode = getSuffixCode(oldUserCode, idUser);

        return nameCode + genderCode + yearCode + suffixCode;
    }
    
    private String getNameCode(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "USE";
        }

        String normalized = java.text.Normalizer
                .normalize(fullName.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D");

        String[] parts = normalized.trim().split("\\s+");

        if (parts.length == 0) {
            return "USE";
        }

        String lastName = parts[parts.length - 1]
                .replaceAll("[^a-zA-Z]", "")
                .toUpperCase();

        if (lastName.isEmpty()) {
            return "USE";
        }

        if (lastName.length() >= 3) {
            return lastName.substring(0, 3);
        }

        return String.format("%-3s", lastName)
                .replace(' ', 'X');
    }
    
    private String getGenderCode(String gender) {
        if (gender == null) {
            return "U";
        }

        return switch (gender) {
            case "Male" -> "M";
            case "Female" -> "F";
            default -> "U";
        };
    }

    private String getYearCode(LocalDate birthday) {
        if (birthday == null) {
            return "01";
        }

        return String.format("%02d", birthday.getYear() % 100);
    }

    private String getSuffixCode(String oldUserCode, String idUser) {
        if (oldUserCode != null && oldUserCode.length() >= 5) {
            return oldUserCode.substring(oldUserCode.length() - 5);
        }

        if (idUser != null && idUser.length() >= 5) {
            return idUser.substring(idUser.length() - 5);
        }

        return "00000";
    }
    
    @Override
    public List<User> findAll() {
        return repository.findAll();
    }
    
    @Override
    public Map<String, Object> getProfile(String idUser) {

        User user = repository.findById(idUser)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng"));

        Map<String, Object> profile = new HashMap<>();

        profile.put("user", user);

        profile.put(
                "authProviders",
                authProviderRepository
                        .findByUser_IdUserAndDeletedAtIsNull(idUser)
        );

        profile.put(
                "addresses",
                addressRepository
                        .findByUser_IdUserAndDeletedAtIsNull(idUser)
        );

        profile.put(
                "defaultAddress",
                user.getDefaultAddress()
        );

        return profile;
    }

    @Override
    public User findById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public User save(User data) {
        return repository.save(data);
    }

    @Override
    public User updateProfile(
            String id,
            UpdateProfileRequest request
    ) {
        User user = findById(id);

        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());

        String newUserCode = generateUserCode(
                user.getFullName(),
                user.getGender(),
                user.getBirthday(),
                user.getUserCode(),
                user.getIdUser()
        );

        user.setUserCode(newUserCode);

        user.setUpdatedAt(LocalDateTime.now());

        return repository.save(user);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
    
    @Override
    public Map<String, Object> uploadAvatar(String idUser, MultipartFile file) {

        User user = repository.findById(idUser)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng"));

        try {

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Vui lòng chọn ảnh");
            }

            String uploadDir = "uploads/avatars/";

            File folder = new File(uploadDir);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            String originalName = file.getOriginalFilename();

            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                );
            }

            String fileName =
                    idUser + "_" +
                    System.currentTimeMillis() +
                    extension;

            Path filePath = Paths.get(uploadDir + fileName);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String avatarUrl =
                    "http://localhost:8080/uploads/avatars/" + fileName;

            user.setAvatar(avatarUrl);

            repository.save(user);

            Map<String, Object> result = new HashMap<>();

            result.put("message", "Upload avatar thành công");
            result.put("avatar", avatarUrl);
            result.put("user", user);

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
