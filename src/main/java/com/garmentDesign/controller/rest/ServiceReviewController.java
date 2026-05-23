package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.ServiceReview;
import com.garmentDesign.service.ServiceReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/service-reviews")
@CrossOrigin(origins = "*")
public class ServiceReviewController {
    private final ServiceReviewService service;

    public ServiceReviewController(ServiceReviewService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceReview> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceReview getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ServiceReview create(@RequestBody ServiceReview data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public ServiceReview update(@PathVariable Long id, @RequestBody ServiceReview data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
