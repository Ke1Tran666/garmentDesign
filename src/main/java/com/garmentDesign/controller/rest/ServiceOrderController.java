package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/service-orders")
@CrossOrigin(origins = "*")
public class ServiceOrderController {
    private final ServiceOrderService service;

    public ServiceOrderController(ServiceOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceOrder> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceOrder getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ServiceOrder create(@RequestBody ServiceOrder data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public ServiceOrder update(@PathVariable Long id, @RequestBody ServiceOrder data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
