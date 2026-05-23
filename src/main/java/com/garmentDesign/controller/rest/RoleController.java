package com.garmentDesign.controller.rest;

import com.garmentDesign.entity.Role;
import com.garmentDesign.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Role> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Role getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Role create(@RequestBody Role data) {
        return service.save(data);
    }

    @PutMapping("/{id}")
    public Role update(@PathVariable Long id, @RequestBody Role data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
