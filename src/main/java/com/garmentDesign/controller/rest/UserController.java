package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.User;
import com.garmentDesign.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> getAll() {
        return service.findAll();
    }

    @GetMapping("/me/{idUser}")
    public ResponseEntity<?> getMyProfile(@PathVariable String idUser) {
        return ResponseEntity.ok(service.getProfile(idUser));
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    public User create(@RequestBody User data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable String id, @RequestBody User data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}