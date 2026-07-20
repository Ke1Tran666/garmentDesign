package com.garmentDesign.controller.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentDesign.service.NewsletterSubscriberService;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterSubscriberController {
	private final NewsletterSubscriberService service;

    public NewsletterSubscriberController(
    		NewsletterSubscriberService service
    ) {
        this.service = service;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestBody Map<String, String> request
    ) {
        try {
            Map<String, Object> response = service.subscribe(
                    request.get("email")
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
