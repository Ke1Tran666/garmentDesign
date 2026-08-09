package com.garmentDesign.service;

import com.garmentDesign.dto.mail.ContactRequest;

public interface MailService {

	void sendSubscribeSuccessEmail(String email);

	void sendContactEmail(ContactRequest request);

	void sendOtpEmail(String email, String otp, String purpose);
}