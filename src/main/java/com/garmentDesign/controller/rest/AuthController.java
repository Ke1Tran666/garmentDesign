package com.garmentDesign.controller.rest;

import com.garmentDesign.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        return ResponseEntity.ok(authService.login(email, password));
    }
    
    @PostMapping("/login-phone")
    public ResponseEntity<?> loginPhone(@RequestBody Map<String, String> body) {

        String phone = body.get("phone");

        return ResponseEntity.ok(authService.loginPhone(phone));
    }
    
    //    Xác Thực Số ĐT
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.sendOtp(body.get("phone")));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.verifyOtp(
                body.get("idUser"),
                body.get("phone"),
                body.get("otp"),
                body.get("mode")
            )
        );
    }
    
    //  Xác Thực Email Local
    @PostMapping("/send-email-otp")
    public ResponseEntity<?> sendEmailOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.sendEmailOtp(body.get("email"))
        );
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<?> verifyEmailOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.verifyEmailOtp(
                body.get("idUser"),
                body.get("email"),
                body.get("otp"),
                body.get("mode")
            )
        );
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {

        return ResponseEntity.ok(
            authService.register(
                body.get("email"),
                body.get("password"),
                body.get("fullName"),
                body.get("gender"),
                body.get("birthday")
            )
        );
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.forgotPassword(body.get("email"))
        );
    }

    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<?> verifyForgotOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.verifyForgotOtp(body.get("email"), body.get("otp"))
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.resetPassword(body.get("email"), body.get("newPassword"))
        );
    }
    
    //    LOGIN GOOGLE
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.googleLogin(body.get("accessToken"))
        );
    }
}