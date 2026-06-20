package com.garmentDesign.service;

import com.garmentDesign.entity.User;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.garmentDesign.dto.user.UpdateProfileRequest;

public interface UserService {
    List<User> findAll();
    User findById(String id);
    User save(User data);
    User updateProfile(String id, UpdateProfileRequest request);
    void delete(String id);
    Map<String, Object> getProfile(String idUser);
    Map<String, Object> uploadAvatar(String idUser, MultipartFile file);
    Map<String, Object> deleteAvatar(String idUser);
    Map<String, Object> changePassword(
            String idUser,
            String oldPassword,
            String newPassword
    );
}
