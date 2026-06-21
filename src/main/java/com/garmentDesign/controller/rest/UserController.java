package com.garmentDesign.controller.rest;

import com.garmentDesign.dto.user.UpdateProfileRequest;
import com.garmentDesign.entity.User;
import com.garmentDesign.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> getAll() {
        return service.findAll();
    }

    @GetMapping("/me/{idUser}")
    public ResponseEntity<?> getMyProfile(@PathVariable String idUser) {
        return ResponseEntity.ok(service.getProfile(idUser));
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    public User create(@RequestBody User data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public User updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request
    ) {
        return service.updateProfile(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping(
            value = "/me/{idUser}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String idUser,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(service.uploadAvatar(idUser, file));
    }
    
    @DeleteMapping("/me/{idUser}/avatar")
    public ResponseEntity<?> deleteAvatar(
            @PathVariable String idUser
    ) {
        return ResponseEntity.ok(service.deleteAvatar(idUser));
    }
    
    // Change Password   
    @PutMapping("/me/{idUser}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable String idUser,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(
                service.changePassword(
                        idUser,
                        body.get("oldPassword"),
                        body.get("newPassword")
                )
        );
    }
    
    // Export user data 
    @GetMapping("/me/{idUser}/export-data")
    public ResponseEntity<?> exportUserData(@PathVariable String idUser) {
        return ResponseEntity.ok(service.exportUserData(idUser));
    }
    
    // Delete account - soft delete
    @DeleteMapping("/me/{idUser}/delete-account")
    public ResponseEntity<?> deleteAccount(@PathVariable String idUser) {
        return ResponseEntity.ok(service.deleteAccount(idUser));
    }
}