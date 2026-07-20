package com.garmentDesign.service.Impl;

import com.garmentDesign.entity.NewsletterSubscriber;
import com.garmentDesign.repository.NewsletterSubscriberRepository;
import com.garmentDesign.service.MailService;
import com.garmentDesign.service.NewsletterSubscriberService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NewsletterSubscriberImpl implements NewsletterSubscriberService {
	private final NewsletterSubscriberRepository repository;
	private final MailService mailService;
	
	public NewsletterSubscriberImpl(
	        NewsletterSubscriberRepository repository,
	        MailService mailService
	) {
	    this.repository = repository;
	    this.mailService = mailService;
	}
    
    @Override
    public Map<String, Object> subscribe(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập email");
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Email không hợp lệ");
        }

        if (repository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new RuntimeException("Email này đã đăng ký nhận tin");
        }

        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        subscriber.setReceiveNotification(true);

        repository.save(subscriber);

        try {
            mailService.sendSubscribeSuccessEmail(email);
        } catch (Exception e) {
            System.err.println(
                "Không thể gửi email xác nhận tới: " + email
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đăng ký nhận tin thành công");

        return response;
    }
}
