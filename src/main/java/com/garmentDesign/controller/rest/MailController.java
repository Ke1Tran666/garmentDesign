package com.garmentDesign.controller.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.garmentDesign.dto.mail.ContactRequest;
import com.garmentDesign.service.MailService;

@RestController
@RequestMapping("/api/mail")
public class MailController {

	private final MailService mailService;

	public MailController(MailService mailService) {
		this.mailService = mailService;
	}

	@PostMapping("/contact")
	public ResponseEntity<Map<String, Object>> sendContact(@RequestBody ContactRequest request) {

		if (request.getFullName() == null || request.getFullName().isBlank()) {
			throw new RuntimeException("Vui lòng nhập họ tên.");
		}

		if (request.getPhone() == null || request.getPhone().isBlank()) {
			throw new RuntimeException("Vui lòng nhập số điện thoại.");
		}

		if (request.getEmail() == null || request.getEmail().isBlank()) {
			throw new RuntimeException("Vui lòng nhập email.");
		}

		if (request.getServiceCode() == null || request.getServiceCode().isBlank()) {
			throw new RuntimeException("Vui lòng chọn dịch vụ.");
		}

		mailService.sendContactEmail(request);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Gửi liên hệ thành công.");

		return ResponseEntity.ok(response);
	}
}