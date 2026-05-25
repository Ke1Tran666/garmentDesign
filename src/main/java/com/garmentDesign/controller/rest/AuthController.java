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
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.sendOtp(body.get("phone")));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.verifyOtp(body.get("phone"), body.get("otp"))
        );
    }
}