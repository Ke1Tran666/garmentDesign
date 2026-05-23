package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.ServiceOrderFile;
import com.garmentDesign.service.ServiceOrderFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/service-order-files")
@CrossOrigin(origins = "*")
public class ServiceOrderFileController {
    private final ServiceOrderFileService service;

    public ServiceOrderFileController(ServiceOrderFileService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceOrderFile> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceOrderFile getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ServiceOrderFile create(@RequestBody ServiceOrderFile data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public ServiceOrderFile update(@PathVariable Long id, @RequestBody ServiceOrderFile data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
