package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.Service;
import com.garmentDesign.service.ServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceController {
    private final ServiceService service;

    public ServiceController(ServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<Service> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Service getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Service create(@RequestBody Service data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public Service update(@PathVariable Long id, @RequestBody Service data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
