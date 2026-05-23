package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.UserAuthProvider;
import com.garmentDesign.service.UserAuthProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user-auth-providers")
@CrossOrigin(origins = "*")
public class UserAuthProviderController {
    private final UserAuthProviderService service;

    public UserAuthProviderController(UserAuthProviderService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAuthProvider> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserAuthProvider getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public UserAuthProvider create(@RequestBody UserAuthProvider data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public UserAuthProvider update(@PathVariable Long id, @RequestBody UserAuthProvider data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
