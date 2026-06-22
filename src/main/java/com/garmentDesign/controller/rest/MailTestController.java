package com.garmentDesign.controller.rest;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
public class MailTestController {

    private final JavaMailSender mailSender;

    public MailTestController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostMapping("/test")
    public Map<String, Object> testMail(@RequestBody Map<String, String> request) {
        String to = request.get("to");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Test gửi mail từ Garment Design");
        message.setText("Xin chào, đây là email test từ Spring Boot.");

        mailSender.send(message);

        return Map.of(
                "success", true,
                "message", "Đã gửi mail test tới " + to
        );
    }
}