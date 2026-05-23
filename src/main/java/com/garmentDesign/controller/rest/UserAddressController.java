package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.service.UserAddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user-addresses")
@CrossOrigin(origins = "*")
public class UserAddressController {
    private final UserAddressService service;

    public UserAddressController(UserAddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAddress> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserAddress getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public UserAddress create(@RequestBody UserAddress data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public UserAddress update(@PathVariable Long id, @RequestBody UserAddress data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
